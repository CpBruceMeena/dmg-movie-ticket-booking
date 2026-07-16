package com.dmg.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TheaterResponse {
    private Long id;
    private String name;
    private String location;
    private Long cityId;
    private String cityName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
