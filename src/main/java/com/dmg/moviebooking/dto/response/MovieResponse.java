package com.dmg.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private Long id;
    private String title;
    private String description;
    private String genre;
    private Integer durationMinutes;
    private String language;
    private LocalDate releaseDate;
    private String posterUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
