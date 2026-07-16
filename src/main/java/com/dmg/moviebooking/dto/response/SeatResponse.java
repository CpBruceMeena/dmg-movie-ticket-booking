package com.dmg.moviebooking.dto.response;

import com.dmg.moviebooking.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private String rowLabel;
    private Integer seatNumber;
    private SeatType seatType;
    private Long screenId;
    private String screenName;
    private LocalDateTime createdAt;
}
