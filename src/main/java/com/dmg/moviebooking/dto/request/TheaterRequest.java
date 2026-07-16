package com.dmg.moviebooking.dto.request;

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
public class TheaterRequest {
    @NotBlank(message = "Theater name is required")
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "City ID is required")
    private Long cityId;
}
