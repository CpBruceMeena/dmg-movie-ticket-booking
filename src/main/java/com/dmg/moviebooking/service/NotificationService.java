package com.dmg.moviebooking.service;

import com.dmg.moviebooking.enums.NotificationEventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for sending notifications throughout the booking lifecycle.
 * Implementations handle the actual delivery (log-based, email, SMS, push, etc.).
 * All methods are designed to be invoked asynchronously to avoid blocking the booking flow.
 */
public interface NotificationService {

    /**
     * Notify user that seats have been held and payment window has started.
     */
    void notifySeatHoldStarted(Long userId, Long bookingId, Long showId, String movieTitle,
                               int seatCount, LocalDateTime holdExpiresAt);

    /**
     * Notify user that booking payment was successful and seats are confirmed.
     */
    void notifyBookingConfirmed(Long userId, Long bookingId, Long showId, String movieTitle,
                                String theaterName, String screenName, LocalDateTime showTime,
                                int seatCount, BigDecimal totalAmount);

    /**
     * Notify user that booking was cancelled.
     */
    void notifyBookingCancelled(Long userId, Long bookingId, Long showId, String movieTitle,
                                String reason);

    /**
     * Notify user that a refund has been processed for a cancelled booking.
     */
    void notifyBookingRefunded(Long userId, Long bookingId, Long showId, String movieTitle,
                               BigDecimal refundAmount);

    /**
     * Notify user that their payment window has expired and the booking was auto-cancelled.
     */
    void notifyPaymentTimeout(Long userId, Long bookingId, Long showId, String movieTitle);

    /**
     * Notify user that their seat hold has expired without payment.
     */
    void notifyHoldExpired(Long userId, Long showId, String movieTitle);

    /**
     * Notify user about an upcoming show (pre-show reminder).
     */
    void notifyShowReminder(Long userId, Long bookingId, Long showId, String movieTitle,
                            String theaterName, String screenName, LocalDateTime showTime);

    /**
     * Generic notification dispatcher for extensibility.
     */
    void sendNotification(Long userId, NotificationEventType eventType, String title, String message);
}
