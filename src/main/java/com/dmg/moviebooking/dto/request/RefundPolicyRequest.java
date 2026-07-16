package com.dmg.moviebooking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPolicyRequest {
    @NotBlank(message = "Policy name is required")
    private String name;

    @NotNull(message = "Hours before show is required")
    @Min(value = 0, message = "Hours must be non-negative")
    private Integer hoursBeforeShow;

    @NotNull(message = "Refund percentage is required")
    @Min(value = 0, message = "Refund percentage must be between 0 and 100")
    @Max(value = 100, message = "Refund percentage must be between 0 and 100")
    private Integer refundPercentage;
}
