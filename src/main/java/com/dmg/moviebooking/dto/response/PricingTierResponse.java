package com.dmg.moviebooking.dto.response;

import com.dmg.moviebooking.enums.SeatType;
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
public class PricingTierResponse {
    private Long id;
    private String name;
    private SeatType seatType;
    private BigDecimal weekdayPrice;
    private BigDecimal weekendPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
