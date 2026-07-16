package com.dmg.moviebooking.service;

import com.dmg.moviebooking.dto.request.BookingRequest;
import com.dmg.moviebooking.dto.response.BookingResponse;
import com.dmg.moviebooking.dto.response.SeatAvailabilityResponse;
import com.dmg.moviebooking.entity.*;
import com.dmg.moviebooking.enums.BookingStatus;
import com.dmg.moviebooking.enums.Role;
import com.dmg.moviebooking.enums.SeatType;
import com.dmg.moviebooking.exception.BookingConflictException;
import com.dmg.moviebooking.exception.InvalidBookingStateException;
import com.dmg.moviebooking.exception.PaymentTimeoutException;
import com.dmg.moviebooking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private UserRepository userRepository;

    private Show testShow;
    private Long userId;
    private Long userId1;
    private Long userId2;

    @BeforeEach
    void setUp() {
        // Create test users for booking ownership
        User user = userRepository.findByUsername("testuser").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("testuser")
                        .email("testuser@test.com")
                        .password("encoded")
                        .fullName("Test User")
                        .role(Role.ROLE_CUSTOMER)
                        .active(true)
                        .build()));
        userId = user.getId();

        User user1 = userRepository.findByUsername("customer1").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("customer1")
                        .email("customer1@test.com")
                        .password("encoded")
                        .fullName("Customer One")
                        .role(Role.ROLE_CUSTOMER)
                        .active(true)
                        .build()));
        userId1 = user1.getId();

        User user2 = userRepository.findByUsername("customer2").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("customer2")
                        .email("customer2@test.com")
                        .password("encoded")
                        .fullName("Customer Two")
                        .role(Role.ROLE_CUSTOMER)
                        .active(true)
                        .build()));
        userId2 = user2.getId();

        // Create test data: City -> Theater -> Screen -> Seats -> Show
        City city = cityRepository.save(City.builder().name("Test City").build());

        Theater theater = theaterRepository.save(Theater.builder()
                .name("Test Theater")
                .location("Test Location")
                .city(city)
                .build());

        Screen screen = screenRepository.save(Screen.builder()
                .name("Test Screen")
                .totalSeats(10)
                .theater(theater)
                .build());

        // Create 10 seats
        for (int i = 1; i <= 10; i++) {
            seatRepository.save(Seat.builder()
                    .rowLabel("A")
                    .seatNumber(i)
                    .seatType(SeatType.REGULAR)
                    .screen(screen)
                    .build());
        }

        testShow = showRepository.save(Show.builder()
                .movieTitle("Inception")
                .screen(screen)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .basePrice(BigDecimal.valueOf(300))
                .build());
    }

    @Test
    void getSeatAvailability_ShouldReturnAllSeatsAsAvailable() {
        List<SeatAvailabilityResponse> availability = bookingService.getSeatAvailability(testShow.getId());

        assertNotNull(availability);
        assertEquals(10, availability.size());
        availability.forEach(seat -> {
            assertEquals(SeatAvailabilityResponse.Status.AVAILABLE, seat.getStatus());
        });
    }

    @Test
    void holdSeats_ShouldCreatePendingPaymentBooking() {
        // Get seat IDs
        List<Seat> seats = seatRepository.findByScreenId(testShow.getScreen().getId());
        Set<Long> seatIds = Set.of(seats.get(0).getId(), seats.get(1).getId(), seats.get(2).getId());

        BookingRequest request = BookingRequest.builder()
                .showId(testShow.getId())
                .seatIds(seatIds)
                .build();

        BookingResponse response = bookingService.holdSeats(request, userId);

        assertNotNull(response);
        assertEquals(BookingStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals(BigDecimal.valueOf(900), response.getTotalAmount()); // 3 seats * 300 base price
        assertEquals(3, response.getSeats().size());
        assertEquals("Inception", response.getMovieTitle());
    }

    @Test
    void holdSeats_ShouldThrowConflict_WhenSeatsAlreadyHeld() {
        List<Seat> seats = seatRepository.findByScreenId(testShow.getScreen().getId());
        Set<Long> seatIds = Set.of(seats.get(0).getId(), seats.get(1).getId());

        // First hold by user1
        bookingService.holdSeats(BookingRequest.builder()
                .showId(testShow.getId())
                .seatIds(seatIds)
                .build(), userId1);

        // Second hold by user2 should fail
        assertThrows(BookingConflictException.class,
                () -> bookingService.holdSeats(BookingRequest.builder()
                        .showId(testShow.getId())
                        .seatIds(seatIds)
                        .build(), userId2));
    }

    @Test
    void holdAndPay_ShouldConfirmBooking() {
        List<Seat> seats = seatRepository.findByScreenId(testShow.getScreen().getId());
        Set<Long> seatIds = Set.of(seats.get(0).getId(), seats.get(1).getId());

        // Hold seats
        BookingResponse heldBooking = bookingService.holdSeats(
                BookingRequest.builder().showId(testShow.getId()).seatIds(seatIds).build(),
                userId);

        // Process payment
        BookingResponse confirmedBooking = bookingService.processPayment(heldBooking.getId(), userId);

        assertEquals(BookingStatus.CONFIRMED, confirmedBooking.getStatus());
        assertNotNull(confirmedBooking.getConfirmedAt());
    }

    @Test
    void holdAndCancel_ShouldReleaseSeats() {
        List<Seat> seats = seatRepository.findByScreenId(testShow.getScreen().getId());
        Set<Long> seatIds = Set.of(seats.get(0).getId());

        // Hold seats
        BookingResponse heldBooking = bookingService.holdSeats(
                BookingRequest.builder().showId(testShow.getId()).seatIds(seatIds).build(),
                userId);

        // Cancel booking
        BookingResponse cancelledBooking = bookingService.cancelBooking(heldBooking.getId(), userId);

        assertEquals(BookingStatus.CANCELLED, cancelledBooking.getStatus());

        // Verify seats are now available
        List<SeatAvailabilityResponse> availability = bookingService.getSeatAvailability(testShow.getId());
        long availableCount = availability.stream()
                .filter(s -> s.getStatus() == SeatAvailabilityResponse.Status.AVAILABLE)
                .count();
        assertEquals(10, availableCount);
    }

    @Test
    void getBookingHistory_ShouldReturnUserBookings() {
        List<Seat> seats = seatRepository.findByScreenId(testShow.getScreen().getId());
        Set<Long> seatIds = Set.of(seats.get(0).getId());

        // Create a booking
        bookingService.holdSeats(
                BookingRequest.builder().showId(testShow.getId()).seatIds(seatIds).build(),
                userId);

        // Get history
        List<BookingResponse> history = bookingService.getBookingHistory(userId);

        assertEquals(1, history.size());
        assertEquals(BookingStatus.PENDING_PAYMENT, history.get(0).getStatus());
    }

    @Test
    void cancelBooking_ShouldThrow_WhenAlreadyCancelled() {
        List<Seat> seats = seatRepository.findByScreenId(testShow.getScreen().getId());
        Set<Long> seatIds = Set.of(seats.get(0).getId());

        BookingResponse heldBooking = bookingService.holdSeats(
                BookingRequest.builder().showId(testShow.getId()).seatIds(seatIds).build(),
                userId);

        bookingService.cancelBooking(heldBooking.getId(), userId);

        assertThrows(InvalidBookingStateException.class,
                () -> bookingService.cancelBooking(heldBooking.getId(), userId));
    }

    @Test
    void seatAvailability_ShouldReflectBookingStatus() {
        List<Seat> seats = seatRepository.findByScreenId(testShow.getScreen().getId());
        Seat seat1 = seats.get(0);
        Seat seat2 = seats.get(1);
        Seat seat3 = seats.get(2);

        // Hold seats 1,2
        bookingService.holdSeats(
                BookingRequest.builder().showId(testShow.getId()).seatIds(Set.of(seat1.getId(), seat2.getId())).build(),
                userId1);

        // Hold and pay for seat 3
        BookingResponse held = bookingService.holdSeats(
                BookingRequest.builder().showId(testShow.getId()).seatIds(Set.of(seat3.getId())).build(),
                userId2);
        bookingService.processPayment(held.getId(), userId2);

        // Check availability
        List<SeatAvailabilityResponse> availability = bookingService.getSeatAvailability(testShow.getId());
        long available = availability.stream().filter(s -> s.getStatus() == SeatAvailabilityResponse.Status.AVAILABLE).count();
        long heldCount = availability.stream().filter(s -> s.getStatus() == SeatAvailabilityResponse.Status.HELD).count();
        long booked = availability.stream().filter(s -> s.getStatus() == SeatAvailabilityResponse.Status.BOOKED).count();

        assertEquals(7, available, "7 seats should be available");
        assertEquals(2, heldCount, "2 seats should be held (seat1, seat2)");
        assertEquals(1, booked, "1 seat should be booked (seat3)");
    }
}
