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
public class ScreenResponse {
    private Long id;
    private String name;
    private Integer totalSeats;
    private Long theaterId;
    private String theaterName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
