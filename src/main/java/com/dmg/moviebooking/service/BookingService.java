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
import com.dmg.moviebooking.repository.BookingRepository;
import com.dmg.moviebooking.repository.BookingSeatRepository;
import com.dmg.moviebooking.repository.SeatRepository;
import com.dmg.moviebooking.service.admin.ShowService;
import org.springframework.cache.CacheManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
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
    private final SeatHoldManager seatHoldManager;
    private final ShowService showService;
    private final CacheManager cacheManager;

    @Value("${booking.hold.duration-minutes:5}")
    private int holdDurationMinutes;

    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "booking-cleanup");
        t.setDaemon(true);
        return t;
    });

    @Value("${booking.hold.cleanup-interval-seconds:30}")
    private int cleanupIntervalSeconds;

    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          SeatRepository seatRepository,
                          SeatHoldManager seatHoldManager,
                          ShowService showService,
                          CacheManager cacheManager) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.seatHoldManager = seatHoldManager;
        this.showService = showService;
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
        List<Seat> seats = seatRepository.findByScreenId(show.getScreen().getId());

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
                            .screenId(seat.getScreen().getId())
                            .status(status)
                            .build();
                })
                .toList();
    }

    /**
     * Hold seats for a user, starting the 5-minute payment window.
     * If seats can't be held (already held by another user), throws BookingConflictException.
     */
    @CacheEvict(value = "seats", allEntries = true)
    public BookingResponse holdSeats(BookingRequest request, String userId) {
        Show show = getShowEntity(request.getShowId());
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());

        if (seats.size() != request.getSeatIds().size()) {
            throw new ResourceNotFoundException("One or more seats not found");
        }

        // Validate all seats belong to the show's screen
        for (Seat seat : seats) {
            if (!seat.getScreen().getId().equals(show.getScreen().getId())) {
                throw new IllegalArgumentException("Seat " + seat.getId() + " does not belong to this show's screen");
            }
        }

        // Try to hold seats in cache
        boolean held = seatHoldManager.holdSeats(
                request.getShowId(), request.getSeatIds(), userId, holdDurationMinutes);
        if (!held) {
            throw new BookingConflictException("Some seats are already held by another user. Please try again.");
        }

        // Calculate total price based on base price (in real system, apply pricing tier logic)
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
                        .booking(booking)
                        .seat(seat)
                        .price(show.getBasePrice())
                        .build())
                .collect(Collectors.toList());

        booking.setBookingSeats(bookingSeats);
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking {} created for user '{}' on show {} with {} seats (hold expires: {})",
                savedBooking.getId(), userId, request.getShowId(), seats.size(), holdExpiresAt);

        return toResponse(savedBooking, show);
    }

    /**
     * Process payment for a booking. Checks if the 5-minute hold window is still valid.
     * If expired, releases the seats and throws PaymentTimeoutException.
     */
    @CacheEvict(value = "seats", allEntries = true)
    public BookingResponse processPayment(Long bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        validateOwnership(booking, userId);
        validateState(booking, BookingStatus.PENDING_PAYMENT, "pay");

        // Check if hold has expired
        if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            // Release seats and cancel booking
            Set<Long> seatIds = new HashSet<>(bookingSeatRepository.findSeatIdsByBookingId(bookingId));
            seatHoldManager.releaseSeats(booking.getShowId(), seatIds);
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(LocalDateTime.now());
            bookingRepository.save(booking);
            log.warn("Payment timeout for booking {} - seats released", bookingId);
            throw new PaymentTimeoutException(
                    "Payment window of " + holdDurationMinutes + " minutes has expired. Booking has been cancelled.");
        }

        // Process payment (mock - in real system, integrate with payment gateway)
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        booking.setHoldExpiresAt(null); // Clear hold expiry since payment is done
        bookingRepository.save(booking);

        Show show = getShowEntity(booking.getShowId());
        log.info("Booking {} confirmed with payment for user '{}'", bookingId, userId);

        return toResponse(booking, show);
    }

    /**
     * Cancel a booking and release the seats.
     */
    @CacheEvict(value = "seats", allEntries = true)
    public BookingResponse cancelBooking(Long bookingId, String userId) {
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
        log.info("Booking {} cancelled by user '{}'", bookingId, userId);

        return toResponse(booking, show);
    }

    /**
     * Get booking history for the current user.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingHistory(String userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(booking -> {
                    Show show = showService.getShowEntity(booking.getShowId());
                    return toResponse(booking, show);
                })
                .toList();
    }

    /**
     * Get a single booking by ID and verify ownership.
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        validateOwnership(booking, userId);
        Show show = getShowEntity(booking.getShowId());
        return toResponse(booking, show);
    }

    /**
     * Cleanup expired PENDING_PAYMENT bookings whose 5-minute hold window has passed.
     * This prevents zombie bookings from holding seats indefinitely.
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
            // Evict seat availability cache for this show
            cacheManager.getCache("seats").evict("availability-" + booking.getShowId());
            log.warn("Auto-cancelled expired booking {} (hold expired at {})", booking.getId(), booking.getHoldExpiresAt());
        }
    }

    private Show getShowEntity(Long showId) {
        return showService.getShowEntity(showId);
    }

    private void validateOwnership(Booking booking, String userId) {
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
        List<BookingResponse.SeatInfo> seatInfos = booking.getBookingSeats().stream()
                .map(bs -> BookingResponse.SeatInfo.builder()
                        .seatId(bs.getSeat().getId())
                        .rowLabel(bs.getSeat().getRowLabel())
                        .seatNumber(bs.getSeat().getSeatNumber())
                        .seatType(bs.getSeat().getSeatType().name())
                        .price(bs.getPrice())
                        .build())
                .toList();

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .movieTitle(show.getMovieTitle())
                .theaterName(show.getScreen().getTheater().getName())
                .screenName(show.getScreen().getName())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .holdExpiresAt(booking.getHoldExpiresAt())
                .confirmedAt(booking.getConfirmedAt())
                .seats(seatInfos)
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
