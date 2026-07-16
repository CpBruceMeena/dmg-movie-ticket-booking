package com.dmg.moviebooking.dto.response;

import com.dmg.moviebooking.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailabilityResponse {
    private Long id;
    private String rowLabel;
    private Integer seatNumber;
    private SeatType seatType;
    private Long screenId;
    private Status status;

    public enum Status {
        AVAILABLE,
        HELD,
        BOOKED
    }
}
