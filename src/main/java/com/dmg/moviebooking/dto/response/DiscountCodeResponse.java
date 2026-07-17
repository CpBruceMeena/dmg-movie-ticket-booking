package com.dmg.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCodeResponse {
    private Long id;
    private String code;
    private BigDecimal discountAmount;
    private boolean active;
    private boolean used;
    private LocalDateTime usedAt;
    private Long usedByUserId;
    private LocalDateTime expiresAt;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
