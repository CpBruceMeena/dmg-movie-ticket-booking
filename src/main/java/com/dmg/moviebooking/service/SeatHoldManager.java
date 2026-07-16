package com.dmg.moviebooking.service;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Manages temporary seat holds during the booking payment window.
 * Implementations can use in-memory cache (local) or Redis (distributed/container deployment).
 */
public interface SeatHoldManager {

    /**
     * Hold seats for a user for the specified duration.
     *
     * @param showId   the show the seats belong to
     * @param seatIds  the seats to hold
     * @param userId   the user ID holding the seats
     * @param durationMinutes how long to hold the seats (e.g. 5 minutes)
     * @return true if all seats were successfully held, false if any were already held by another user
     */
    boolean holdSeats(Long showId, Set<Long> seatIds, Long userId, int durationMinutes);

    /**
     * Release a held seat (after payment timeout, cancellation, or successful payment).
     */
    void releaseSeats(Long showId, Set<Long> seatIds);

    /**
     * Release all seats held by a user for a specific show.
     */
    void releaseSeatsByUser(Long showId, Long userId);

    /**
     * Check if a specific seat is currently held by another user.
     */
    boolean isSeatHeld(Long showId, Long seatId, Long excludeUserId);

    /**
     * Get the set of seat IDs held by a user for a show.
     */
    Set<Long> getHeldSeatsByUser(Long showId, Long userId);

    /**
     * Get the set of all held seat IDs for a show (across all users).
     */
    Set<Long> getAllHeldSeatIds(Long showId);

    /**
     * Get the expiry time for a held seat.
     */
    LocalDateTime getHoldExpiry(Long showId, Long seatId);

    /**
     * Get the user ID holding a specific seat.
     */
    Long getHoldingUser(Long showId, Long seatId);

    /**
     * Extend the hold duration for a set of seats (e.g. when payment is initiated).
     */
    boolean extendHold(Long showId, Set<Long> seatIds, Long userId, int additionalMinutes);

    /**
     * Clean up all expired holds.
     */
    void cleanupExpiredHolds();
}
