package com.dmg.moviebooking.dto.response;

import com.dmg.moviebooking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long userId;
    private Long showId;
    private String movieTitle;
    private String theaterName;
    private String screenName;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime holdExpiresAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime refundedAt;
    private BigDecimal refundAmount;
    private Long discountCodeId;
    private BigDecimal discountAmount;
    private List<SeatInfo> seats;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatInfo {
        private Long seatId;
        private String rowLabel;
        private Integer seatNumber;
        private String seatType;
        private BigDecimal price;
    }
}
