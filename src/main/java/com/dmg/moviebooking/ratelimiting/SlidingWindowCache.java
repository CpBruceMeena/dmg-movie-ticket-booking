package com.dmg.moviebooking.ratelimiting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class SlidingWindowCache {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowCache.class);

    private final int maxRequests;
    private final long windowMillis;
    private final Map<String, SlidingWindow> cache = new ConcurrentHashMap<>();

    public SlidingWindowCache(
            @Value("${rate.limit.max-requests}") int maxRequests,
            @Value("${rate.limit.window-minutes}") long windowMinutes) {
        this.maxRequests = maxRequests;
        this.windowMillis = TimeUnit.MINUTES.toMillis(windowMinutes);

        // Schedule cleanup every minute to remove stale entries
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    public boolean isAllowed(String key) {
        SlidingWindow window = cache.computeIfAbsent(key, k -> new SlidingWindow());
        return window.allowRequest();
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minusMillis(windowMillis);
        cache.entrySet().removeIf(entry -> {
            boolean stale = entry.getValue().isOlderThan(cutoff);
            if (stale) {
                log.debug("Cleaned up rate limit entry for key: {}", entry.getKey());
            }
            return stale;
        });
    }

    private class SlidingWindow {
        private static final int MAX_TIMESTAMPS = 10000;
        private final ConcurrentHashMap<Instant, Boolean> timestamps = new ConcurrentHashMap<>();

        boolean allowRequest() {
            Instant now = Instant.now();
            Instant windowStart = now.minusMillis(windowMillis);

            // Evict old timestamps
            timestamps.keySet().removeIf(ts -> ts.isBefore(windowStart));

            // Count requests in current window
            if (timestamps.size() >= maxRequests) {
                return false;
            }

            // Record this request
            if (timestamps.size() < MAX_TIMESTAMPS) {
                timestamps.put(now, Boolean.TRUE);
            }
            return true;
        }

        boolean isOlderThan(Instant cutoff) {
            return timestamps.keySet().stream().allMatch(ts -> ts.isBefore(cutoff));
        }
    }
}
