package com.dmg.moviebooking.controller.admin;

import com.dmg.moviebooking.dto.request.SeatLayoutRequest;
import com.dmg.moviebooking.dto.response.SeatResponse;
import com.dmg.moviebooking.service.admin.SeatLayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/seats")
@Tag(name = "Admin - Seat Layout", description = "Admin endpoints for seat layout configuration")
public class SeatLayoutController {

    private final SeatLayoutService seatLayoutService;

    public SeatLayoutController(SeatLayoutService seatLayoutService) {
        this.seatLayoutService = seatLayoutService;
    }

    @PostMapping("/layout")
    @Operation(summary = "Configure seat layout for a screen")
    public ResponseEntity<List<SeatResponse>> configureLayout(@Valid @RequestBody SeatLayoutRequest request) {
        List<SeatResponse> seats = seatLayoutService.configureLayout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(seats);
    }

    @GetMapping
    @Operation(summary = "Get seats by screen ID")
    public ResponseEntity<List<SeatResponse>> getSeatsByScreen(@RequestParam Long screenId) {
        return ResponseEntity.ok(seatLayoutService.getSeatsByScreenId(screenId));
    }
}
