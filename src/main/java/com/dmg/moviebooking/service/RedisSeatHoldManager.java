package com.dmg.moviebooking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis-based implementation of SeatHoldManager for distributed seat hold management.
 * Uses Redis hashes per show for efficient batch operations and TTL-based auto-expiry.
 *
 * Key structure:
 *   seat:holds:{showId} -> Hash(seatId -> JSON{userId, expiresAt})
 *   seat:hold:{showId}:{seatId} -> String JSON (individual key with TTL)
 */
@Service
@ConditionalOnProperty(name = "booking.hold.manager", havingValue = "redis", matchIfMissing = false)
public class RedisSeatHoldManager implements SeatHoldManager {

    private static final Logger log = LoggerFactory.getLogger(RedisSeatHoldManager.class);

    private static final String HOLDS_KEY_PREFIX = "seat:holds:";
    private static final String HOLD_KEY_PREFIX = "seat:hold:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSeatHoldManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public boolean holdSeats(Long showId, Set<Long> seatIds, Long userId, int durationMinutes) {
        String holdsKey = HOLDS_KEY_PREFIX + showId;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(durationMinutes);

        // Check if any seat is already held by another user
        List<Object> fieldKeys = seatIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        List<Object> rawHoldJsons = redisTemplate.opsForHash().multiGet(holdsKey, fieldKeys);

        for (int i = 0; i < seatIds.size(); i++) {
            Object rawJson = rawHoldJsons.get(i);
            if (rawJson != null) {
                try {
                    HoldData existing = objectMapper.readValue((String) rawJson, HoldData.class);
                    if (!existing.userId.equals(userId) && existing.expiresAt.isAfter(now)) {
                        return false; // Held by another user and not expired
                    }
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse hold data for seat on show {}", showId, e);
                }
            }
        }

        // Hold all seats using pipelining for performance
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long seatId : seatIds) {
                String field = String.valueOf(seatId);
                HoldData holdData = new HoldData(userId, expiresAt);
                try {
                    String holdJson = objectMapper.writeValueAsString(holdData);
                    redisTemplate.opsForHash().put(holdsKey, field, holdJson);

                    // Set individual key with TTL for auto-expiry
                    String individualKey = HOLD_KEY_PREFIX + showId + ":" + seatId;
                    redisTemplate.opsForValue().set(individualKey, holdJson,
                            durationMinutes, TimeUnit.MINUTES);
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize hold data for seat {}", seatId, e);
                }
            }
            return null;
        });

        // Set TTL on the hash (renewed with each hold operation)
        redisTemplate.expire(holdsKey, Duration.ofMinutes(durationMinutes));

        log.debug("Held seats {} for user id '{}' on show {} until {}",
                seatIds, userId, showId, expiresAt);
        return true;
    }

    @Override
    public void releaseSeats(Long showId, Set<Long> seatIds) {
        String holdsKey = HOLDS_KEY_PREFIX + showId;

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long seatId : seatIds) {
                String field = String.valueOf(seatId);
                redisTemplate.opsForHash().delete(holdsKey, field);

                // Also remove individual key
                String individualKey = HOLD_KEY_PREFIX + showId + ":" + seatId;
                redisTemplate.delete(individualKey);
            }
            return null;
        });

        log.debug("Released seats {} on show {}", seatIds, showId);
    }

    @Override
    public void releaseSeatsByUser(Long showId, Long userId) {
        Set<Long> userSeats = getHeldSeatsByUser(showId, userId);
        if (!userSeats.isEmpty()) {
            releaseSeats(showId, userSeats);
            log.debug("Released seats {} for user id '{}' on show {}", userSeats, userId, showId);
        }
    }

    @Override
    public boolean isSeatHeld(Long showId, Long seatId, Long excludeUserId) {
        String individualKey = HOLD_KEY_PREFIX + showId + ":" + seatId;
        String json = redisTemplate.opsForValue().get(individualKey);
        if (json == null) return false;

        try {
            HoldData hold = objectMapper.readValue(json, HoldData.class);
            return !hold.userId.equals(excludeUserId) && hold.expiresAt.isAfter(LocalDateTime.now());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse hold data for seat {} on show {}", seatId, showId, e);
            return false;
        }
    }

    @Override
    public Set<Long> getHeldSeatsByUser(Long showId, Long userId) {
        String holdsKey = HOLDS_KEY_PREFIX + showId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(holdsKey);
        if (entries.isEmpty()) return Set.of();

        Set<Long> userSeats = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                HoldData hold = objectMapper.readValue((String) entry.getValue(), HoldData.class);
                if (hold.userId.equals(userId) && hold.expiresAt.isAfter(now)) {
                    userSeats.add(Long.valueOf((String) entry.getKey()));
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse hold data for seat {} on show {}", entry.getKey(), showId, e);
            }
        }

        return userSeats;
    }

    @Override
    public Set<Long> getAllHeldSeatIds(Long showId) {
        String holdsKey = HOLDS_KEY_PREFIX + showId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(holdsKey);
        if (entries.isEmpty()) return Set.of();

        Set<Long> heldSeats = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                HoldData hold = objectMapper.readValue((String) entry.getValue(), HoldData.class);
                if (hold.expiresAt.isAfter(now)) {
                    heldSeats.add(Long.valueOf((String) entry.getKey()));
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse hold data for seat {} on show {}", entry.getKey(), showId, e);
            }
        }

        return heldSeats;
    }

    @Override
    public LocalDateTime getHoldExpiry(Long showId, Long seatId) {
        String individualKey = HOLD_KEY_PREFIX + showId + ":" + seatId;
        String json = redisTemplate.opsForValue().get(individualKey);
        if (json == null) return null;

        try {
            HoldData hold = objectMapper.readValue(json, HoldData.class);
            return hold.expiresAt;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse hold data for seat {} on show {}", seatId, showId, e);
            return null;
        }
    }

    @Override
    public Long getHoldingUser(Long showId, Long seatId) {
        String individualKey = HOLD_KEY_PREFIX + showId + ":" + seatId;
        String json = redisTemplate.opsForValue().get(individualKey);
        if (json == null) return null;

        try {
            HoldData hold = objectMapper.readValue(json, HoldData.class);
            return hold.userId;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse hold data for seat {} on show {}", seatId, showId, e);
            return null;
        }
    }

    @Override
    public boolean extendHold(Long showId, Set<Long> seatIds, Long userId, int additionalMinutes) {
        String holdsKey = HOLDS_KEY_PREFIX + showId;
        LocalDateTime now = LocalDateTime.now();

        for (Long seatId : seatIds) {
            String field = String.valueOf(seatId);
            String json = (String) redisTemplate.opsForHash().get(holdsKey, field);
            if (json == null) return false;

            try {
                HoldData existing = objectMapper.readValue(json, HoldData.class);
                if (!existing.userId.equals(userId)) return false;

                // Update with extended expiry
                LocalDateTime newExpiry = existing.expiresAt.plusMinutes(additionalMinutes);
                HoldData updated = new HoldData(userId, newExpiry);
                String updatedJson = objectMapper.writeValueAsString(updated);
                redisTemplate.opsForHash().put(holdsKey, field, updatedJson);

                // Update individual key TTL
                String individualKey = HOLD_KEY_PREFIX + showId + ":" + seatId;
                long ttlMinutes = Duration.between(now, newExpiry).toMinutes();
                if (ttlMinutes > 0) {
                    redisTemplate.opsForValue().set(individualKey, updatedJson, ttlMinutes, TimeUnit.MINUTES);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse hold data for seat {} on show {}", seatId, showId, e);
                return false;
            }
        }

        return true;
    }

    @Override
    public void cleanupExpiredHolds() {
        // Redis TTL handles auto-expiry of individual keys.
        // For the hash entries, we rely on the hash-level TTL.
        log.debug("Redis seat hold manager: cleanup is handled by Redis TTL");
    }

    /**
     * Internal data class for serializing hold information to/from Redis.
     */
    private record HoldData(Long userId, LocalDateTime expiresAt) {}
}
