package com.dmg.moviebooking.service;

import com.dmg.moviebooking.dto.request.BookingRequest;
import com.dmg.moviebooking.dto.response.BookingResponse;
import com.dmg.moviebooking.dto.response.SeatAvailabilityResponse;
import com.dmg.moviebooking.entity.*;
import com.dmg.moviebooking.enums.BookingStatus;
import com.dmg.moviebooking.exception.BookingConflictException;
import com.dmg.moviebooking.exception.InvalidBookingStateException;
import com.dmg.moviebooking.exception.PaymentTimeoutException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.*;
import com.dmg.moviebooking.service.admin.DiscountCodeService;
import com.dmg.moviebooking.service.admin.MovieService;
import com.dmg.moviebooking.service.admin.RefundPolicyService;
import com.dmg.moviebooking.service.admin.ShowService;
import org.springframework.cache.CacheManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;
    private final TheaterRepository theaterRepository;
    private final UserRepository userRepository;
    private final SeatHoldManager seatHoldManager;
    private final ShowService showService;
    private final MovieRepository movieRepository;
    private final RefundPolicyService refundPolicyService;
    private final DiscountCodeService discountCodeService;
    private final NotificationService notificationService;
    private final CacheManager cacheManager;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Value("${booking.hold.duration-minutes:5}")
    private int holdDurationMinutes;

    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "booking-cleanup");
        t.setDaemon(true);
        return t;
    });

    @Value("${booking.hold.cleanup-interval-seconds:30}")
    private int cleanupIntervalSeconds;

    @Value("${booking.reminder.before-show-minutes:60}")
    private int reminderBeforeShowMinutes;

    @Value("${booking.reminder.interval-minutes:30}")
    private int reminderIntervalMinutes;

    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          SeatRepository seatRepository,
                          ScreenRepository screenRepository,
                          TheaterRepository theaterRepository,
                          UserRepository userRepository,
                          SeatHoldManager seatHoldManager,
                          ShowService showService,
                          MovieRepository movieRepository,
                          RefundPolicyService refundPolicyService,
                          DiscountCodeService discountCodeService,
                          NotificationService notificationService,
                          CacheManager cacheManager) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.screenRepository = screenRepository;
        this.theaterRepository = theaterRepository;
        this.userRepository = userRepository;
        this.seatHoldManager = seatHoldManager;
        this.showService = showService;
        this.movieRepository = movieRepository;
        this.refundPolicyService = refundPolicyService;
        this.discountCodeService = discountCodeService;
        this.notificationService = notificationService;
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    public void init() {
        cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredBookings,
                cleanupIntervalSeconds,
                cleanupIntervalSeconds,
                TimeUnit.SECONDS
        );
        log.info("Booking cleanup scheduler started (interval: {}s)", cleanupIntervalSeconds);
    }

    @PreDestroy
    public void destroy() {
        cleanupScheduler.shutdown();
        log.info("Booking cleanup scheduler shutdown");
    }

    /**
     * Get seat availability for a show - shows which seats are AVAILABLE, HELD, or BOOKED.
     */
    @Cacheable(value = "seats", key = "'availability-' + #showId")
    @Transactional(readOnly = true)
    public List<SeatAvailabilityResponse> getSeatAvailability(Long showId) {
        Show show = getShowEntity(showId);
        List<Seat> seats = seatRepository.findByScreenId(show.getScreenId());

        Set<Long> heldSeatIds = seatHoldManager.getAllHeldSeatIds(showId);
        Set<Long> bookedSeatIds = new HashSet<>(bookingSeatRepository.findBookedSeatIdsByShowId(showId));

        return seats.stream()
                .map(seat -> {
                    SeatAvailabilityResponse.Status status;
                    if (bookedSeatIds.contains(seat.getId())) {
                        status = SeatAvailabilityResponse.Status.BOOKED;
                    } else if (heldSeatIds.contains(seat.getId())) {
                        status = SeatAvailabilityResponse.Status.HELD;
                    } else {
                        status = SeatAvailabilityResponse.Status.AVAILABLE;
                    }
                    return SeatAvailabilityResponse.builder()
                            .id(seat.getId())
                            .rowLabel(seat.getRowLabel())
                            .seatNumber(seat.getSeatNumber())
                            .seatType(seat.getSeatType())
                            .screenId(seat.getScreenId())
                            .status(status)
                            .build();
                })
                .toList();
    }

    /**
     * Hold seats for a user, starting the 5-minute payment window.
     */
    @CacheEvict(value = "seats", allEntries = true)
    public BookingResponse holdSeats(BookingRequest request, Long userId) {
        Show show = getShowEntity(request.getShowId());
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());

        if (seats.size() != request.getSeatIds().size()) {
            throw new ResourceNotFoundException("One or more seats not found");
        }

        // Validate all seats belong to the show's screen
        for (Seat seat : seats) {
            if (!seat.getScreenId().equals(show.getScreenId())) {
                throw new IllegalArgumentException("Seat " + seat.getId() + " does not belong to this show's screen");
            }
        }

        // Try to hold seats in cache
        boolean held = seatHoldManager.holdSeats(
                request.getShowId(), request.getSeatIds(), userId, holdDurationMinutes);
        if (!held) {
            throw new BookingConflictException("Some seats are already held by another user. Please try again.");
        }

        // Calculate total price based on base price
        BigDecimal totalAmount = show.getBasePrice().multiply(BigDecimal.valueOf(seats.size()));

        LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(holdDurationMinutes);

        // Create booking in PENDING_PAYMENT status
        Booking booking = Booking.builder()
                .userId(userId)
                .showId(request.getShowId())
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(totalAmount)
                .holdExpiresAt(holdExpiresAt)
                .build();

        List<BookingSeat> bookingSeats = seats.stream()
                .map(seat -> BookingSeat.builder()
                        .bookingId(booking.getId())
                        .seatId(seat.getId())
                        .price(show.getBasePrice())
                        .build())
                .collect(Collectors.toList());

        Booking savedBooking = bookingRepository.save(booking);

        // Now save booking seats with the booking ID
        for (BookingSeat bs : bookingSeats) {
            bs.setBookingId(savedBooking.getId());
        }
        bookingSeatRepository.saveAll(bookingSeats);

        log.info("Booking {} created for user '{}' on show {} with {} seats (hold expires: {})",
                savedBooking.getId(), userId, request.getShowId(), seats.size(), holdExpiresAt);

        String movietitle = getMovieTitle(show);

        // Send async notification
        notificationService.notifySeatHoldStarted(
                userId, savedBooking.getId(), show.getId(), movietitle,
                seats.size(), holdExpiresAt);

        return toResponse(savedBooking, show);
    }

    /**
     * Process payment for a booking, optionally applying a discount code.
     * If the discount code is invalid, expired, or already used, the payment will be rejected.
     * If payment succeeds, the discount code is marked as used.
     */
    @CacheEvict(value = "seats", allEntries = true)
    public BookingResponse processPayment(Long bookingId, Long userId, String discountCode) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        validateOwnership(booking, userId);
        validateState(booking, BookingStatus.PENDING_PAYMENT, "pay");

        // Check if hold has expired
        if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            Show timeoutShow = getShowEntity(booking.getShowId());
            Set<Long> seatIds = new HashSet<>(bookingSeatRepository.findSeatIdsByBookingId(bookingId));
            seatHoldManager.releaseSeats(booking.getShowId(), seatIds);
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(LocalDateTime.now());
            bookingRepository.save(booking);

            String movietitle = getMovieTitle(timeoutShow);
            // Send async notification
            notificationService.notifyPaymentTimeout(
                    userId, bookingId, timeoutShow.getId(), movietitle);

            log.warn("Payment timeout for booking {} - seats released", bookingId);
            throw new PaymentTimeoutException(
                    "Payment window of " + holdDurationMinutes + " minutes has expired. Booking has been cancelled.");
        }

        // Validate and apply discount code if provided
        BigDecimal discountAmount = BigDecimal.ZERO;
        com.dmg.moviebooking.entity.DiscountCode appliedCode = null;
        if (discountCode != null && !discountCode.isBlank()) {
            appliedCode = discountCodeService.validateAndGetCode(discountCode);
            discountAmount = appliedCode.getDiscountAmount();
            // Ensure discount doesn't exceed total
            if (discountAmount.compareTo(booking.getTotalAmount()) > 0) {
                discountAmount = booking.getTotalAmount();
            }
            log.info("Applying discount code '{}' (₹{}) to booking {}",
                    discountCode, discountAmount, bookingId);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        booking.setHoldExpiresAt(null);
        booking.setDiscountAmount(discountAmount);
        if (appliedCode != null) {
            booking.setDiscountCodeId(appliedCode.getId());
        }
        bookingRepository.save(booking);

        // Mark discount code as used after successful payment
        if (appliedCode != null) {
            discountCodeService.markCodeAsUsed(appliedCode.getId(), userId);
        }

        Show show = getShowEntity(booking.getShowId());

        BigDecimal finalAmount = booking.getTotalAmount().subtract(discountAmount);
        log.info("Booking {} confirmed with payment for user id '{}' (original: ₹{}, discount: ₹{}, final: ₹{})",
                bookingId, userId, booking.getTotalAmount(), discountAmount, finalAmount);

        // Send async notification
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        String screenName = screenRepository.findById(show.getScreenId())
                .map(Screen::getName).orElse("Unknown");
        String theaterName = screenRepository.findById(show.getScreenId())
                .flatMap(screen -> theaterRepository.findById(screen.getTheaterId()))
                .map(Theater::getName).orElse("Unknown");
        notificationService.notifyBookingConfirmed(
                userId, bookingId, show.getId(), getMovieTitle(show),
                theaterName, screenName, show.getStartTime(),
                bookingSeats.size(), finalAmount);

        return toResponse(booking, show);
    }

    /**
     * Cancel a booking and release the seats.
     */
    @CacheEvict(value = "seats", allEntries = true)
    public BookingResponse cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        validateOwnership(booking, userId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.REFUNDED) {
            throw new InvalidBookingStateException("Booking has already been refunded");
        }

        // Release seat holds in cache
        Set<Long> seatIds = new HashSet<>(bookingSeatRepository.findSeatIdsByBookingId(bookingId));
        seatHoldManager.releaseSeats(booking.getShowId(), seatIds);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        Show show = getShowEntity(booking.getShowId());
        log.info("Booking {} cancelled by user id '{}'", bookingId, userId);

        // Send async notification
        notificationService.notifyBookingCancelled(
                userId, bookingId, show.getId(), getMovieTitle(show),
                "Cancelled by user");

        return toResponse(booking, show);
    }

    /**
     * Refund a confirmed booking by calculating the applicable refund amount
     * based on cancellation time relative to the show's start time and configured refund policies.
     * Releases the seats back to the pool.
     */
    @CacheEvict(value = "seats", allEntries = true)
    public BookingResponse refundBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        validateOwnership(booking, userId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException(
                    "Cannot refund booking in " + booking.getStatus() + " state (expected: " + BookingStatus.CONFIRMED + ")");
        }

        Show show = getShowEntity(booking.getShowId());

        // Calculate time until show start with full precision
        java.time.Duration timeUntilShow = java.time.Duration.between(LocalDateTime.now(), show.getStartTime());

        // Find the applicable refund policy
        RefundPolicy policy = refundPolicyService.findApplicablePolicy(timeUntilShow);

        BigDecimal refundPercent = (policy != null)
                ? BigDecimal.valueOf(policy.getRefundPercentage())
                : BigDecimal.ZERO;

        BigDecimal refundAmount = booking.getTotalAmount()
                .multiply(refundPercent)
                .divide(HUNDRED, 2, java.math.RoundingMode.HALF_UP);

        log.info("Refund for booking {}: {}h before show, policy '{}' ({}%), refund amount: ₹{} (total: ₹{})",
                bookingId, timeUntilShow.toHours(),
                policy != null ? policy.getName() : "NONE",
                refundPercent,
                refundAmount.toPlainString(),
                booking.getTotalAmount().toPlainString());

        // Release seats
        Set<Long> seatIds = new HashSet<>(bookingSeatRepository.findSeatIdsByBookingId(bookingId));
        seatHoldManager.releaseSeats(booking.getShowId(), seatIds);

        // Update booking status
        booking.setStatus(BookingStatus.REFUNDED);
        booking.setRefundedAt(LocalDateTime.now());
        booking.setRefundAmount(refundAmount);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Send async notification
        notificationService.notifyBookingRefunded(
                userId, bookingId, show.getId(), getMovieTitle(show), refundAmount);

        return toResponse(booking, show);
    }

    /**
     * Get booking history for the current user.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingHistory(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(booking -> {
                    Show show = getShowEntity(booking.getShowId());
                    return toResponse(booking, show);
                })
                .toList();
    }

    /**
     * Get a single booking by ID and verify ownership.
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        validateOwnership(booking, userId);
        Show show = getShowEntity(booking.getShowId());
        return toResponse(booking, show);
    }

    /**
     * Cleanup expired PENDING_PAYMENT bookings.
     */
    void cleanupExpiredBookings() {
        List<Booking> expired = bookingRepository.findByStatusAndHoldExpiresAtBefore(
                BookingStatus.PENDING_PAYMENT, LocalDateTime.now());
        for (Booking booking : expired) {
            Set<Long> seatIds = new HashSet<>(bookingSeatRepository.findSeatIdsByBookingId(booking.getId()));
            seatHoldManager.releaseSeats(booking.getShowId(), seatIds);
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(LocalDateTime.now());
            bookingRepository.save(booking);
            cacheManager.getCache("seats").evict("availability-" + booking.getShowId());
            log.warn("Auto-cancelled expired booking {} (hold expired at {})", booking.getId(), booking.getHoldExpiresAt());

            // Send async notification for expired hold
            Show expiredShow = getShowEntity(booking.getShowId());
            notificationService.notifyHoldExpired(
                    booking.getUserId(), booking.getShowId(), getMovieTitle(expiredShow));
        }
    }

    /**
     * Periodically send pre-show reminders for upcoming confirmed bookings.
     * Runs at fixed intervals based on configuration.
     */
    @Scheduled(fixedDelayString = "${booking.reminder.interval-ms:1800000}")
    public void sendShowReminders() {
        log.debug("Checking for upcoming shows to send reminders...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowStart = now.plusMinutes(reminderBeforeShowMinutes);
        LocalDateTime reminderWindowEnd = reminderWindowStart.plusMinutes(reminderIntervalMinutes);

        // Find all shows starting within the reminder window
        List<Show> upcomingShows = showService.getShowsStartingBetween(reminderWindowStart, reminderWindowEnd);
        if (upcomingShows.isEmpty()) {
            return;
        }

        List<Long> showIds = upcomingShows.stream().map(Show::getId).toList();
        List<Booking> confirmedBookings = bookingRepository.findByStatusAndShowIdIn(BookingStatus.CONFIRMED, showIds);

        for (Booking booking : confirmedBookings) {
            Show show = upcomingShows.stream()
                    .filter(s -> s.getId().equals(booking.getShowId()))
                    .findFirst()
                    .orElse(null);
            if (show == null) continue;

            String screenName = screenRepository.findById(show.getScreenId())
                    .map(Screen::getName).orElse("Unknown");
            String theaterName = screenRepository.findById(show.getScreenId())
                    .flatMap(screen -> theaterRepository.findById(screen.getTheaterId()))
                    .map(Theater::getName).orElse("Unknown");

            notificationService.notifyShowReminder(
                    booking.getUserId(), booking.getId(), show.getId(),
                    getMovieTitle(show), theaterName, screenName, show.getStartTime());

            log.info("Pre-show reminder sent for booking {} (user: {}, show: '{}' at {})",
                    booking.getId(), booking.getUserId(), getMovieTitle(show), show.getStartTime());
        }
    }

    private String getMovieTitle(Show show) {
        return movieRepository.findById(show.getMovieId())
                .map(Movie::getTitle)
                .orElse("Unknown");
    }

    private Show getShowEntity(Long showId) {
        return showService.getShowEntity(showId);
    }

    private void validateOwnership(Booking booking, Long userId) {
        if (!booking.getUserId().equals(userId)) {
            throw new InvalidBookingStateException("Booking does not belong to this user");
        }
    }

    private void validateState(Booking booking, BookingStatus expectedStatus, String action) {
        if (booking.getStatus() != expectedStatus) {
            throw new InvalidBookingStateException(
                    "Cannot " + action + " booking in " + booking.getStatus() + " state (expected: " + expectedStatus + ")");
        }
    }

    private BookingResponse toResponse(Booking booking, Show show) {
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());

        // Look up show metadata
        String movieTitle = getMovieTitle(show);
        String screenName = screenRepository.findById(show.getScreenId())
                .map(Screen::getName)
                .orElse("Unknown");
        String theaterName = screenRepository.findById(show.getScreenId())
                .flatMap(screen -> theaterRepository.findById(screen.getTheaterId()))
                .map(theater -> theater.getName())
                .orElse("Unknown");

        List<BookingResponse.SeatInfo> seatInfos = bookingSeats.stream()
                .map(bs -> {
                    Seat seat = seatRepository.findById(bs.getSeatId()).orElse(null);
                    return BookingResponse.SeatInfo.builder()
                            .seatId(bs.getSeatId())
                            .rowLabel(seat != null ? seat.getRowLabel() : "?")
                            .seatNumber(seat != null ? seat.getSeatNumber() : 0)
                            .seatType(seat != null ? seat.getSeatType().name() : "REGULAR")
                            .price(bs.getPrice())
                            .build();
                })
                .toList();

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .movieTitle(movieTitle)
                .theaterName(theaterName)
                .screenName(screenName)
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .holdExpiresAt(booking.getHoldExpiresAt())
                .confirmedAt(booking.getConfirmedAt())
                .refundedAt(booking.getRefundedAt())
                .refundAmount(booking.getRefundAmount())
                .discountCodeId(booking.getDiscountCodeId())
                .discountAmount(booking.getDiscountAmount())
                .seats(seatInfos)
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
