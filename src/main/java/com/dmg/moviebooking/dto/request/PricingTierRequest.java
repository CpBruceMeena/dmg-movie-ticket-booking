package com.dmg.moviebooking.dto.request;

import com.dmg.moviebooking.enums.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingTierRequest {
    @NotBlank(message = "Tier name is required")
    private String name;

    @NotNull(message = "Seat type is required")
    private SeatType seatType;

    @NotNull(message = "Weekday price is required")
    @Positive(message = "Weekday price must be positive")
    private BigDecimal weekdayPrice;

    @NotNull(message = "Weekend price is required")
    @Positive(message = "Weekend price must be positive")
    private BigDecimal weekendPrice;
}
