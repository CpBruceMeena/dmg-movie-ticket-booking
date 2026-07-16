package com.dmg.moviebooking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySeatHoldManagerTest {

    private InMemorySeatHoldManager holdManager;

    @BeforeEach
    void setUp() {
        holdManager = new InMemorySeatHoldManager();
    }

    @Test
    void holdSeats_ShouldSucceed_WhenSeatsAreAvailable() {
        boolean result = holdManager.holdSeats(1L, Set.of(1L, 2L, 3L), 100L, 5);
        assertTrue(result);
    }

    @Test
    void holdSeats_ShouldFail_WhenSeatsAlreadyHeldByAnotherUser() {
        holdManager.holdSeats(1L, Set.of(1L, 2L), 100L, 5);

        boolean result = holdManager.holdSeats(1L, Set.of(2L, 3L), 200L, 5);
        assertFalse(result);
    }

    @Test
    void holdSeats_ShouldSucceed_WhenSeatsHeldBySameUser() {
        holdManager.holdSeats(1L, Set.of(1L, 2L), 100L, 5);

        boolean result = holdManager.holdSeats(1L, Set.of(1L, 2L, 3L), 100L, 5);
        assertTrue(result);
    }

    @Test
    void releaseSeats_ShouldRemoveHolds() {
        holdManager.holdSeats(1L, Set.of(1L, 2L, 3L), 100L, 5);

        holdManager.releaseSeats(1L, Set.of(1L, 2L));

        assertFalse(holdManager.isSeatHeld(1L, 1L, 100L));
        assertFalse(holdManager.isSeatHeld(1L, 2L, 100L));
        assertTrue(holdManager.isSeatHeld(1L, 3L, 999L));
    }

    @Test
    void getAllHeldSeatIds_ShouldReturnOnlyActiveHolds() {
        holdManager.holdSeats(1L, Set.of(1L, 2L, 3L), 100L, 5);
        holdManager.holdSeats(2L, Set.of(4L, 5L), 200L, 5);

        Set<Long> show1Held = holdManager.getAllHeldSeatIds(1L);
        Set<Long> show2Held = holdManager.getAllHeldSeatIds(2L);

        assertEquals(Set.of(1L, 2L, 3L), show1Held);
        assertEquals(Set.of(4L, 5L), show2Held);
    }

    @Test
    void getHeldSeatsByUser_ShouldReturnOnlyUserHolds() {
        holdManager.holdSeats(1L, Set.of(1L, 2L), 100L, 5);
        holdManager.holdSeats(1L, Set.of(3L, 4L), 200L, 5);

        Set<Long> user1Held = holdManager.getHeldSeatsByUser(1L, 100L);
        Set<Long> user2Held = holdManager.getHeldSeatsByUser(1L, 200L);

        assertEquals(Set.of(1L, 2L), user1Held);
        assertEquals(Set.of(3L, 4L), user2Held);
    }

    @Test
    void releaseSeatsByUser_ShouldRemoveOnlyThatUsersHolds() {
        holdManager.holdSeats(1L, Set.of(1L, 2L), 100L, 5);
        holdManager.holdSeats(1L, Set.of(3L, 4L), 200L, 5);

        holdManager.releaseSeatsByUser(1L, 100L);

        assertFalse(holdManager.isSeatHeld(1L, 1L, 999L));
        assertFalse(holdManager.isSeatHeld(1L, 2L, 999L));
        assertTrue(holdManager.isSeatHeld(1L, 3L, 999L));
        assertTrue(holdManager.isSeatHeld(1L, 4L, 999L));
    }

    @Test
    void getHoldingUser_ShouldReturnCorrectUser() {
        holdManager.holdSeats(1L, Set.of(1L), 100L, 5);

        assertEquals(Long.valueOf(100L), holdManager.getHoldingUser(1L, 1L));
        assertNull(holdManager.getHoldingUser(1L, 999L));
    }

    @Test
    void extendHold_ShouldExtendDuration() {
        holdManager.holdSeats(1L, Set.of(1L, 2L), 100L, 5);

        boolean result = holdManager.extendHold(1L, Set.of(1L, 2L), 100L, 5);

        assertTrue(result);
    }

    @Test
    void extendHold_ShouldFail_WhenSeatHeldByDifferentUser() {
        holdManager.holdSeats(1L, Set.of(1L), 100L, 5);

        boolean result = holdManager.extendHold(1L, Set.of(1L), 200L, 5);

        assertFalse(result);
    }

    @Test
    void cleanupExpiredHolds_ShouldRemoveExpiredEntries() {
        holdManager.holdSeats(1L, Set.of(1L, 2L), 100L, -1); // Already expired

        holdManager.cleanupExpiredHolds();

        assertTrue(holdManager.getAllHeldSeatIds(1L).isEmpty());
    }
}
