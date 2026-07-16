package com.dmg.moviebooking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of SeatHoldManager for distributed seat hold management.
 * Uses Lua scripts for atomic check-and-set operations to prevent race conditions.
 *
 * Key structure:
 *   seat:holds:{showId} -> Hash(seatId -> JSON{userId, expiresAt})
 *   seat:hold:{showId}:{seatId} -> String JSON (individual key with TTL for auto-expiry)
 */
@Service
@ConditionalOnProperty(name = "booking.hold.manager", havingValue = "redis", matchIfMissing = false)
public class RedisSeatHoldManager implements SeatHoldManager {

    private static final Logger log = LoggerFactory.getLogger(RedisSeatHoldManager.class);

    private static final String HOLDS_KEY_PREFIX = "seat:holds:";
    private static final String HOLD_KEY_PREFIX = "seat:hold:";

    /**
     * Lua script for atomic seat hold check-and-set.
     *
     * KEYS[1] = seat:holds:{showId}
     * ARGV[1] = JSON array of seat IDs
     * ARGV[2] = current timestamp (ISO 8601 string)
     * ARGV[3] = expiry timestamp (ISO 8601 string)
     * ARGV[4] = duration in seconds
     * ARGV[5] = user ID (number)
     * ARGV[6] = show ID
     *
     * Returns 1 on success, 0 if any seat is already held by another user.
     */
    private static final String ATOMIC_HOLD_SCRIPT =
            "local holds_key = KEYS[1]" +
            "local seat_ids_json = ARGV[1]" +
            "local now_str = ARGV[2]" +
            "local expiry_str = ARGV[3]" +
            "local duration_sec = tonumber(ARGV[4])" +
            "local user_id = tonumber(ARGV[5])" +
            "local show_id = ARGV[6]" +
            "" +
            "local seats = cjson.decode(seat_ids_json)" +
            "" +
            "-- Check each seat: if held by another user and not expired, fail" +
            "for i, seat_id in ipairs(seats) do" +
            "    local seat_id_str = tostring(seat_id)" +
            "    local existing = redis.call('HGET', holds_key, seat_id_str)" +
            "    if existing then" +
            "        local existing_data = cjson.decode(existing)" +
            "        if existing_data.userId ~= user_id and" +
            "           existing_data.expiresAt > now_str then" +
            "            return 0" +
            "        end" +
            "    end" +
            "end" +
            "" +
            "-- All checks passed: hold all seats atomically" +
            "for i, seat_id in ipairs(seats) do" +
            "    local seat_id_str = tostring(seat_id)" +
            "    local hold_data = cjson.encode({userId = user_id, expiresAt = expiry_str})" +
            "    local individual_key = 'seat:hold:' .. show_id .. ':' .. seat_id_str" +
            "    redis.call('HSET', holds_key, seat_id_str, hold_data)" +
            "    redis.call('SETEX', individual_key, duration_sec, hold_data)" +
            "end" +
            "" +
            "redis.call('EXPIRE', holds_key, duration_sec)" +
            "" +
            "return 1";

    /**
     * Lua script for atomic seat hold extension.
     *
     * KEYS[1] = seat:holds:{showId}
     * ARGV[1] = seat ID
     * ARGV[2] = user ID (number)
     * ARGV[3] = new expiry timestamp (ISO 8601 string)
     * ARGV[4] = additional duration in seconds
     * ARGV[5] = show ID
     *
     * Returns 1 on success, 0 if seat not found or held by different user.
     */
    private static final String ATOMIC_EXTEND_SCRIPT =
            "local holds_key = KEYS[1]" +
            "local seat_id = ARGV[1]" +
            "local user_id = tonumber(ARGV[2])" +
            "local new_expiry_str = ARGV[3]" +
            "local additional_sec = tonumber(ARGV[4])" +
            "local show_id = ARGV[5]" +
            "" +
            "local existing = redis.call('HGET', holds_key, seat_id)" +
            "if not existing then" +
            "    return 0" +
            "end" +
            "" +
            "local existing_data = cjson.decode(existing)" +
            "if existing_data.userId ~= user_id then" +
            "    return 0" +
            "end" +
            "" +
            "-- Update with new expiry" +
            "local hold_data = cjson.encode({userId = user_id, expiresAt = new_expiry_str})" +
            "local individual_key = 'seat:hold:' .. show_id .. ':' .. seat_id" +
            "redis.call('HSET', holds_key, seat_id, hold_data)" +
            "redis.call('SETEX', individual_key, additional_sec, hold_data)" +
            "" +
            "return 1";

    /**
     * Lua script for atomic seat release (delete from hash and individual key).
     *
     * KEYS[1] = seat:holds:{showId}
     * ARGV[1] = seat ID
     * ARGV[2] = show ID
     */
    /**
     * Lua script for atomic batch release of seats (delete from hash and individual keys).
     *
     * KEYS[1] = seat:holds:{showId}
     * ARGV[1] = JSON array of seat IDs
     * ARGV[2] = show ID
     */
    private static final String ATOMIC_RELEASE_SCRIPT =
            "local holds_key = KEYS[1]" +
            "local seat_ids_json = ARGV[1]" +
            "local show_id = ARGV[2]" +
            "" +
            "local seats = cjson.decode(seat_ids_json)" +
            "" +
            "for i, seat_id in ipairs(seats) do" +
            "    local seat_id_str = tostring(seat_id)" +
            "    redis.call('HDEL', holds_key, seat_id_str)" +
            "    local individual_key = 'seat:hold:' .. show_id .. ':' .. seat_id_str" +
            "    redis.call('DEL', individual_key)" +
            "end" +
            "" +
            "return 1";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<Long> atomicHoldScript;
    private final DefaultRedisScript<Long> atomicExtendScript;
    private final DefaultRedisScript<Long> atomicReleaseScript;

    private final HealthService healthService;

    public RedisSeatHoldManager(StringRedisTemplate redisTemplate,
                                 HealthService healthService) {
        this.redisTemplate = redisTemplate;
        this.healthService = healthService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        this.atomicHoldScript = new DefaultRedisScript<>();
        this.atomicHoldScript.setScriptText(ATOMIC_HOLD_SCRIPT);
        this.atomicHoldScript.setResultType(Long.class);

        this.atomicExtendScript = new DefaultRedisScript<>();
        this.atomicExtendScript.setScriptText(ATOMIC_EXTEND_SCRIPT);
        this.atomicExtendScript.setResultType(Long.class);

        this.atomicReleaseScript = new DefaultRedisScript<>();
        this.atomicReleaseScript.setScriptText(ATOMIC_RELEASE_SCRIPT);
        this.atomicReleaseScript.setResultType(Long.class);
    }

    @PostConstruct
    public void verifyConnection() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            boolean connected = "PONG".equalsIgnoreCase(pong);
            healthService.setRedisHealthy(connected);
            if (connected) {
                log.info("Redis connection verified at startup - PONG received");
            } else {
                log.warn("Redis ping at startup returned unexpected: {}", pong);
            }
        } catch (Exception e) {
            healthService.setRedisHealthy(false);
            log.error("Redis connection failed at startup: {}", e.getMessage());
        }
    }

    @Override
    public boolean holdSeats(Long showId, Set<Long> seatIds, Long userId, int durationMinutes) {
        String holdsKey = HOLDS_KEY_PREFIX + showId;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(durationMinutes);

        String seatIdsJson;
        try {
            seatIdsJson = objectMapper.writeValueAsString(seatIds);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize seat IDs for Lua script", e);
            return false;
        }

        String nowStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String expiresAtStr = expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        long durationSeconds = TimeUnit.MINUTES.toSeconds(durationMinutes);

        // Execute Lua script atomically on the Redis server
        Long result = redisTemplate.execute(
                atomicHoldScript,
                List.of(holdsKey),
                seatIdsJson,
                nowStr,
                expiresAtStr,
                String.valueOf(durationSeconds),
                String.valueOf(userId),
                String.valueOf(showId)
        );

        boolean success = result != null && result == 1L;
        if (success) {
            log.debug("Atomically held seats {} for user id '{}' on show {} until {}",
                    seatIds, userId, showId, expiresAt);
        } else {
            log.debug("Failed to hold seats {} for user id '{}' on show {} - conflict detected",
                    seatIds, userId, showId);
        }
        return success;
    }

    @Override
    public void releaseSeats(Long showId, Set<Long> seatIds) {
        if (seatIds.isEmpty()) return;

        String holdsKey = HOLDS_KEY_PREFIX + showId;
        String seatIdsJson;
        try {
            seatIdsJson = objectMapper.writeValueAsString(seatIds);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize seat IDs for release on show {}", showId, e);
            return;
        }

        redisTemplate.execute(
                atomicReleaseScript,
                List.of(holdsKey),
                seatIdsJson,
                String.valueOf(showId)
        );

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
        LocalDateTime newExpiry = LocalDateTime.now().plusMinutes(additionalMinutes);
        String newExpiryStr = newExpiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        long additionalSeconds = TimeUnit.MINUTES.toSeconds(additionalMinutes);

        for (Long seatId : seatIds) {
            Long result = redisTemplate.execute(
                    atomicExtendScript,
                    List.of(HOLDS_KEY_PREFIX + showId),
                    String.valueOf(seatId),
                    String.valueOf(userId),
                    newExpiryStr,
                    String.valueOf(additionalSeconds),
                    String.valueOf(showId)
            );

            if (result == null || result != 1L) {
                return false;
            }
        }

        log.debug("Extended hold for seats {} for user id '{}' on show {} by {} minutes",
                seatIds, userId, showId, additionalMinutes);
        return true;
    }

    @Override
    public void cleanupExpiredHolds() {
        // Redis TTL handles auto-expiry. No additional cleanup needed.
        log.debug("Redis seat hold manager: cleanup is handled by Redis TTL");
    }

    /**
     * Internal data class for serializing hold information to/from Redis.
     * userId is stored as JSON number (Long), expiresAt as ISO 8601 string.
     */
    private record HoldData(Long userId, LocalDateTime expiresAt) {}
}
