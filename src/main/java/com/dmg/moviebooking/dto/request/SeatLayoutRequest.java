package com.dmg.moviebooking.dto.request;

import com.dmg.moviebooking.enums.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayoutRequest {
    @NotNull(message = "Screen ID is required")
    private Long screenId;

    @Min(value = 1, message = "Number of rows must be at least 1")
    private Integer rows;

    @Min(value = 1, message = "Seats per row must be at least 1")
    private Integer seatsPerRow;

    @NotNull(message = "Seat type is required")
    private SeatType seatType;
}
