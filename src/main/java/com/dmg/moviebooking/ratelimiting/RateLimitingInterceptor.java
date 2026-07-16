package com.dmg.moviebooking.ratelimiting;

import com.dmg.moviebooking.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingInterceptor.class);

    private final SlidingWindowCache slidingWindowCache;

    public RateLimitingInterceptor(SlidingWindowCache slidingWindowCache) {
        this.slidingWindowCache = slidingWindowCache;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // Skip rate limiting for auth endpoints
        String path = request.getRequestURI();
        if (path.equals("/api/auth/login")) {
            return true;
        }

        String key = resolveKey(request);

        if (!slidingWindowCache.isAllowed(key)) {
            log.warn("Rate limit exceeded for key: {}", key);
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Please try again later.");
        }

        return true;
    }

    private String resolveKey(HttpServletRequest request) {
        // Use authenticated username if available, otherwise fall back to IP
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        String ip = request.getRemoteAddr();
        if (ip == null) {
            ip = "unknown";
        }
        return "ip:" + ip;
    }
}
