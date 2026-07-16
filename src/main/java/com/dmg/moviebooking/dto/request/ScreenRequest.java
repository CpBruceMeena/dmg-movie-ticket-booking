package com.dmg.moviebooking.dto.request;

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
public class ScreenRequest {
    @NotBlank(message = "Screen name is required")
    private String name;

    @NotNull(message = "Theater ID is required")
    private Long theaterId;

    @Min(value = 1, message = "Total seats must be at least 1")
    private Integer totalSeats;
}
