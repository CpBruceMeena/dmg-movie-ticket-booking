package com.dmg.moviebooking.service;

import com.dmg.moviebooking.entity.User;
import com.dmg.moviebooking.enums.NotificationEventType;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Log-based notification service that writes notification events to the application log.
 * This serves as a placeholder for real notification delivery (email, SMS, push, etc.)
 * and allows the notification contracts to be verified without external dependencies.
 */
@Service
public class LogNotificationService implements NotificationService {

    private static final Logger notificationLog = LoggerFactory.getLogger("NOTIFICATION");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserRepository userRepository;

    public LogNotificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Async
    @Override
    public void notifySeatHoldStarted(Long userId, Long bookingId, Long showId, String movieTitle,
                                      int seatCount, LocalDateTime holdExpiresAt) {
        String username = resolveUsername(userId);
        String message = String.format(
                "HOLD STARTED | User: %s (%d) | Booking: %d | Show: %d | Movie: %s | Seats: %d | Hold expires: %s",
                username, userId, bookingId, showId, movieTitle, seatCount, holdExpiresAt.format(FORMATTER)
        );
        notificationLog.info("[SEAT_HOLD] {}", message);
    }

    @Async
    @Override
    public void notifyBookingConfirmed(Long userId, Long bookingId, Long showId, String movieTitle,
                                       String theaterName, String screenName, LocalDateTime showTime,
                                       int seatCount, BigDecimal totalAmount) {
        String username = resolveUsername(userId);
        String message = String.format(
                "BOOKING CONFIRMED | User: %s (%d) | Booking: %d | Show: %d | Movie: %s | Theater: %s | Screen: %s | Show: %s | Seats: %d | Amount: ₹%s",
                username, userId, bookingId, showId, movieTitle, theaterName, screenName,
                showTime.format(FORMATTER), seatCount, totalAmount.toPlainString()
        );
        notificationLog.info("[BOOKING_CONFIRMED] {}", message);
    }

    @Async
    @Override
    public void notifyBookingCancelled(Long userId, Long bookingId, Long showId, String movieTitle,
                                       String reason) {
        String username = resolveUsername(userId);
        String message = String.format(
                "BOOKING CANCELLED | User: %s (%d) | Booking: %d | Show: %d | Movie: %s | Reason: %s",
                username, userId, bookingId, showId, movieTitle, reason
        );
        notificationLog.warn("[BOOKING_CANCELLED] {}", message);
    }

    @Async
    @Override
    public void notifyBookingRefunded(Long userId, Long bookingId, Long showId, String movieTitle,
                                      BigDecimal refundAmount) {
        String username = resolveUsername(userId);
        String message = String.format(
                "REFUND PROCESSED | User: %s (%d) | Booking: %d | Show: %d | Movie: %s | Refund: ₹%s",
                username, userId, bookingId, showId, movieTitle, refundAmount.toPlainString()
        );
        notificationLog.info("[BOOKING_REFUNDED] {}", message);
    }

    @Async
    @Override
    public void notifyPaymentTimeout(Long userId, Long bookingId, Long showId, String movieTitle) {
        String username = resolveUsername(userId);
        String message = String.format(
                "PAYMENT TIMEOUT | User: %s (%d) | Booking: %d | Show: %d | Movie: %s",
                username, userId, bookingId, showId, movieTitle
        );
        notificationLog.warn("[PAYMENT_TIMEOUT] {}", message);
    }

    @Async
    @Override
    public void notifyHoldExpired(Long userId, Long showId, String movieTitle) {
        String username = resolveUsername(userId);
        String message = String.format(
                "HOLD EXPIRED | User: %s (%d) | Show: %d | Movie: %s — seats released",
                username, userId, showId, movieTitle
        );
        notificationLog.warn("[HOLD_EXPIRED] {}", message);
    }

    @Async
    @Override
    public void notifyShowReminder(Long userId, Long bookingId, Long showId, String movieTitle,
                                   String theaterName, String screenName, LocalDateTime showTime) {
        String username = resolveUsername(userId);
        String message = String.format(
                "SHOW REMINDER | User: %s (%d) | Booking: %d | Show: %d | Movie: %s | Theater: %s | Screen: %s | Show: %s",
                username, userId, bookingId, showId, movieTitle, theaterName, screenName, showTime.format(FORMATTER)
        );
        notificationLog.info("[SHOW_REMINDER] {}", message);
    }

    @Async
    @Override
    public void sendNotification(Long userId, NotificationEventType eventType, String title, String message) {
        String username = resolveUsername(userId);
        notificationLog.info("[{}] User: {} ({}) | Title: {} | Message: {}",
                eventType, username, userId, title, message);
    }

    private String resolveUsername(Long userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse("unknown");
    }
}
