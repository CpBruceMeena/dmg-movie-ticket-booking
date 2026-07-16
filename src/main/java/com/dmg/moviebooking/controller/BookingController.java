package com.dmg.moviebooking.controller;

import com.dmg.moviebooking.dto.request.BookingRequest;
import com.dmg.moviebooking.dto.response.BookingResponse;
import com.dmg.moviebooking.dto.response.SeatAvailabilityResponse;
import com.dmg.moviebooking.dto.response.ShowResponse;
import com.dmg.moviebooking.service.BookingService;
import com.dmg.moviebooking.service.admin.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Customer - Booking", description = "Customer endpoints for browsing shows and managing bookings")
public class BookingController {

    private final BookingService bookingService;
    private final ShowService showService;

    public BookingController(BookingService bookingService, ShowService showService) {
        this.bookingService = bookingService;
        this.showService = showService;
    }

    // ==================== Public Endpoints ====================

    @GetMapping("/shows")
    @Operation(summary = "Browse all shows (public)")
    public ResponseEntity<List<ShowResponse>> browseShows(
            @RequestParam(required = false) Long theaterId) {
        if (theaterId != null) {
            return ResponseEntity.ok(showService.getShowsByTheaterId(theaterId));
        }
        return ResponseEntity.ok(showService.getAllShows());
    }

    @GetMapping("/shows/{showId}/seats")
    @Operation(summary = "Get seat availability for a show (public)")
    public ResponseEntity<List<SeatAvailabilityResponse>> getSeatAvailability(@PathVariable Long showId) {
        return ResponseEntity.ok(bookingService.getSeatAvailability(showId));
    }

    // ==================== Authenticated Endpoints ====================

    @PostMapping("/bookings/hold")
    @Operation(summary = "Hold seats and start the 5-minute payment window")
    public ResponseEntity<BookingResponse> holdSeats(@Valid @RequestBody BookingRequest request,
                                                     Authentication authentication) {
        String userId = authentication.getName();
        BookingResponse response = bookingService.holdSeats(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bookings/{bookingId}/pay")
    @Operation(summary = "Process payment for held seats (must be within 5-minute window)")
    public ResponseEntity<BookingResponse> processPayment(@PathVariable Long bookingId,
                                                           Authentication authentication) {
        String userId = authentication.getName();
        BookingResponse response = bookingService.processPayment(bookingId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    @Operation(summary = "Cancel a booking and release the seats")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long bookingId,
                                                          Authentication authentication) {
        String userId = authentication.getName();
        BookingResponse response = bookingService.cancelBooking(bookingId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get booking history for the authenticated user")
    public ResponseEntity<List<BookingResponse>> getBookingHistory(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }

    @GetMapping("/bookings/{bookingId}")
    @Operation(summary = "Get booking details by ID")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long bookingId,
                                                           Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(bookingService.getBookingById(bookingId, userId));
    }
}
