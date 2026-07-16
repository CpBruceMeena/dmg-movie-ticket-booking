package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.SeatLayoutRequest;
import com.dmg.moviebooking.dto.response.SeatResponse;
import com.dmg.moviebooking.entity.Screen;
import com.dmg.moviebooking.entity.Seat;
import com.dmg.moviebooking.repository.ScreenRepository;
import com.dmg.moviebooking.repository.SeatRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SeatLayoutService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;

    public SeatLayoutService(SeatRepository seatRepository, ScreenRepository screenRepository) {
        this.seatRepository = seatRepository;
        this.screenRepository = screenRepository;
    }

    @CacheEvict(value = "seats", allEntries = true)
    public List<SeatResponse> configureLayout(SeatLayoutRequest request) {
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new RuntimeException("Screen not found: " + request.getScreenId()));

        List<Seat> seats = new ArrayList<>();
        for (int row = 0; row < request.getRows(); row++) {
            char rowLabel = (char) ('A' + row);
            for (int seatNum = 1; seatNum <= request.getSeatsPerRow(); seatNum++) {
                Seat seat = Seat.builder()
                        .rowLabel(String.valueOf(rowLabel))
                        .seatNumber(seatNum)
                        .seatType(request.getSeatType())
                        .screenId(request.getScreenId())
                        .build();
                seats.add(seat);
            }
        }

        seats = seatRepository.saveAll(seats);

        // Update total seats on screen
        long totalSeats = seatRepository.countByScreenId(request.getScreenId());
        screen.setTotalSeats((int) totalSeats);
        screenRepository.save(screen);

        return seats.stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "seats", key = "#screenId")
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByScreenId(Long screenId) {
        return seatRepository.findByScreenId(screenId).stream()
                .map(this::toResponse)
                .toList();
    }

    private SeatResponse toResponse(Seat seat) {
        String screenName = screenRepository.findById(seat.getScreenId())
                .map(Screen::getName)
                .orElse("Unknown");

        return SeatResponse.builder()
                .id(seat.getId())
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .screenId(seat.getScreenId())
                .screenName(screenName)
                .createdAt(seat.getCreatedAt())
                .build();
    }
}
