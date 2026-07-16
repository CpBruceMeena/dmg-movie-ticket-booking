package com.dmg.moviebooking.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class InMemorySeatHoldManager implements SeatHoldManager {

    private static final Logger log = LoggerFactory.getLogger(InMemorySeatHoldManager.class);

    // Nested map: showId -> (seatId -> HoldInfo)
    private final Map<Long, Map<Long, HoldInfo>> holdsByShow = new ConcurrentHashMap<>();

    private record HoldInfo(Long userId, LocalDateTime expiresAt) {}

    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "seat-hold-cleanup");
        t.setDaemon(true);
        return t;
    });

    @Value("${booking.hold.cleanup-interval-seconds:30}")
    private int cleanupIntervalSeconds;

    @PostConstruct
    public void init() {
        cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredHolds,
                cleanupIntervalSeconds,
                cleanupIntervalSeconds,
                TimeUnit.SECONDS
        );
        log.info("Seat hold cleanup scheduler started (interval: {}s)", cleanupIntervalSeconds);
    }

    @PreDestroy
    public void shutdown() {
        cleanupScheduler.shutdown();
        log.info("Seat hold cleanup scheduler shutdown");
    }

    @Override
    public boolean holdSeats(Long showId, Set<Long> seatIds, Long userId, int durationMinutes) {
        Map<Long, HoldInfo> showHolds = holdsByShow.computeIfAbsent(showId, k -> new ConcurrentHashMap<>());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(durationMinutes);

        // Check if any seat is already held by another user
        for (Long seatId : seatIds) {
            HoldInfo existing = showHolds.get(seatId);
            if (existing != null && !existing.userId.equals(userId) && existing.expiresAt.isAfter(now)) {
                return false; // Seat held by another user and not expired
            }
        }

        // Hold all seats atomically
        for (Long seatId : seatIds) {
            showHolds.put(seatId, new HoldInfo(userId, expiresAt));
        }
        log.debug("Held seats {} for user id '{}' on show {} until {}", seatIds, userId, showId, expiresAt);
        return true;
    }

    @Override
    public void releaseSeats(Long showId, Set<Long> seatIds) {
        Map<Long, HoldInfo> showHolds = holdsByShow.get(showId);
        if (showHolds != null) {
            seatIds.forEach(showHolds::remove);
            log.debug("Released seats {} on show {}", seatIds, showId);
        }
    }

    @Override
    public void releaseSeatsByUser(Long showId, Long userId) {
        Map<Long, HoldInfo> showHolds = holdsByShow.get(showId);
        if (showHolds != null) {
            Set<Long> userSeats = showHolds.entrySet().stream()
                    .filter(e -> e.getValue().userId.equals(userId))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            userSeats.forEach(showHolds::remove);
            if (!userSeats.isEmpty()) {
                log.debug("Released seats {} for user id '{}' on show {}", userSeats, userId, showId);
            }
        }
    }

    @Override
    public boolean isSeatHeld(Long showId, Long seatId, Long excludeUserId) {
        Map<Long, HoldInfo> showHolds = holdsByShow.get(showId);
        if (showHolds == null) return false;
        HoldInfo hold = showHolds.get(seatId);
        return hold != null
                && !hold.userId.equals(excludeUserId)
                && hold.expiresAt.isAfter(LocalDateTime.now());
    }

    @Override
    public Set<Long> getHeldSeatsByUser(Long showId, Long userId) {
        Map<Long, HoldInfo> showHolds = holdsByShow.get(showId);
        if (showHolds == null) return Set.of();
        LocalDateTime now = LocalDateTime.now();
        return showHolds.entrySet().stream()
                .filter(e -> e.getValue().userId.equals(userId) && e.getValue().expiresAt.isAfter(now))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getAllHeldSeatIds(Long showId) {
        Map<Long, HoldInfo> showHolds = holdsByShow.get(showId);
        if (showHolds == null) return Set.of();
        LocalDateTime now = LocalDateTime.now();
        return showHolds.entrySet().stream()
                .filter(e -> e.getValue().expiresAt.isAfter(now))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public LocalDateTime getHoldExpiry(Long showId, Long seatId) {
        Map<Long, HoldInfo> showHolds = holdsByShow.get(showId);
        if (showHolds == null) return null;
        HoldInfo hold = showHolds.get(seatId);
        return hold != null ? hold.expiresAt : null;
    }

    @Override
    public Long getHoldingUser(Long showId, Long seatId) {
        Map<Long, HoldInfo> showHolds = holdsByShow.get(showId);
        if (showHolds == null) return null;
        HoldInfo hold = showHolds.get(seatId);
        return hold != null ? hold.userId : null;
    }

    @Override
    public boolean extendHold(Long showId, Set<Long> seatIds, Long userId, int additionalMinutes) {
        Map<Long, HoldInfo> showHolds = holdsByShow.get(showId);
        if (showHolds == null) return false;
        LocalDateTime now = LocalDateTime.now();

        for (Long seatId : seatIds) {
            HoldInfo existing = showHolds.get(seatId);
            if (existing == null || !existing.userId.equals(userId)) {
                return false;
            }
            showHolds.put(seatId, new HoldInfo(userId, existing.expiresAt.plusMinutes(additionalMinutes)));
        }
        return true;
    }

    @Override
    public void cleanupExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        int cleaned = 0;
        for (Map.Entry<Long, Map<Long, HoldInfo>> showEntry : holdsByShow.entrySet()) {
            Long showId = showEntry.getKey();
            Map<Long, HoldInfo> showHolds = showEntry.getValue();
            Set<Long> expired = showHolds.entrySet().stream()
                    .filter(e -> e.getValue().expiresAt.isBefore(now))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            expired.forEach(showHolds::remove);
            cleaned += expired.size();
            if (!expired.isEmpty()) {
                log.debug("Cleaned {} expired holds for show {}", expired.size(), showId);
            }
        }
        if (cleaned > 0) {
            log.info("Cleaned {} expired seat holds", cleaned);
        }
    }

}
