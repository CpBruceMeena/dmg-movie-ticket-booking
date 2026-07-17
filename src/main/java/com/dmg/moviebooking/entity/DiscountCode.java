package com.dmg.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One-time-use discount code that applies a fixed amount off the booking total.
 * Once a payment is successfully processed using this code, it is marked as used
 * and cannot be reused. If a payment fails or times out, the code remains valid.
 */
@Entity
@Table(name = "discount_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "used_by_user_id")
    private Long usedByUserId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
