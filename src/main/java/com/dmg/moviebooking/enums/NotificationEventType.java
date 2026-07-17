package com.dmg.moviebooking.enums;

/**
 * Types of notification events that can be triggered throughout the booking lifecycle.
 * Each event corresponds to a specific user-facing notification.
 */
public enum NotificationEventType {
    SEAT_HOLD_STARTED,
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    BOOKING_REFUNDED,
    PAYMENT_TIMEOUT,
    HOLD_EXPIRED,
    SHOW_REMINDER
}
