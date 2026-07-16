package com.dmg.moviebooking.service;

import com.dmg.moviebooking.dto.response.HealthResponse;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service that checks the health of external dependencies (database, Redis, etc.).
 */
@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final EntityManager entityManager;
    private final StringRedisTemplate redisTemplate;
    private final boolean redisConfigured;
    private volatile boolean redisHealthy = false;

    public HealthService(EntityManager entityManager,
                         StringRedisTemplate redisTemplate,
                         @Value("${booking.hold.manager:redis}") String holdManager) {
        this.entityManager = entityManager;
        this.redisTemplate = redisTemplate;
        this.redisConfigured = "redis".equalsIgnoreCase(holdManager);
    }

    /**
     * Called by RedisSeatHoldManager on startup to report Redis health.
     */
    public void setRedisHealthy(boolean healthy) {
        this.redisHealthy = healthy;
        if (healthy) {
            log.info("Redis connection verified successfully");
        } else {
            log.warn("Redis connection check failed");
        }
    }

    /**
     * Perform a comprehensive health check of all services.
     */
    public HealthResponse checkHealth() {
        Map<String, HealthResponse.ServiceHealth> services = new LinkedHashMap<>();
        boolean allHealthy = true;

        // Check database connectivity
        HealthResponse.ServiceHealth dbHealth = checkDatabase();
        services.put("database", dbHealth);
        if (!"UP".equals(dbHealth.getStatus())) {
            allHealthy = false;
        }

        // Check Redis connectivity (only if configured)
        if (redisConfigured) {
            HealthResponse.ServiceHealth redisHealth = checkRedis();
            services.put("redis", redisHealth);
            if (!"UP".equals(redisHealth.getStatus())) {
                allHealthy = false;
            }
        } else {
            services.put("redis", HealthResponse.ServiceHealth.builder()
                    .status("DISABLED")
                    .message("Redis not configured (using in-memory seat hold manager)")
                    .build());
        }

        String overallStatus = allHealthy ? "UP" : "DEGRADED";
        if (services.values().stream().allMatch(s -> "UP".equals(s.getStatus()))) {
            overallStatus = "UP";
        } else if (services.values().stream().allMatch(s -> "DOWN".equals(s.getStatus()))) {
            overallStatus = "DOWN";
        } else {
            overallStatus = "DEGRADED";
        }

        return HealthResponse.builder()
                .status(overallStatus)
                .timestamp(LocalDateTime.now())
                .services(services)
                .build();
    }

    private HealthResponse.ServiceHealth checkDatabase() {
        try {
            // Execute a simple query to verify DB connectivity
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return HealthResponse.ServiceHealth.builder()
                    .status("UP")
                    .message("Database is reachable")
                    .build();
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            return HealthResponse.ServiceHealth.builder()
                    .status("DOWN")
                    .message("Database is unreachable: " + e.getMessage())
                    .build();
        }
    }

    private HealthResponse.ServiceHealth checkRedis() {
        try {
            String pong = redisTemplate.execute((RedisCallback<String>) connection -> {
                return connection.ping();
            });
            boolean isPong = "PONG".equalsIgnoreCase(pong);
            return HealthResponse.ServiceHealth.builder()
                    .status(isPong ? "UP" : "DEGRADED")
                    .message(isPong ? "Redis is reachable" : "Redis ping returned unexpected: " + pong)
                    .build();
        } catch (Exception e) {
            log.error("Redis health check failed: {}", e.getMessage());
            return HealthResponse.ServiceHealth.builder()
                    .status(redisHealthy ? "DEGRADED" : "DOWN")
                    .message("Redis is unreachable: " + e.getMessage())
                    .build();
        }
    }
}
