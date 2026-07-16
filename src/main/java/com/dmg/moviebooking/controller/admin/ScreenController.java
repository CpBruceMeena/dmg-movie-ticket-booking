package com.dmg.moviebooking.controller.admin;

import com.dmg.moviebooking.dto.request.ScreenRequest;
import com.dmg.moviebooking.dto.response.ScreenResponse;
import com.dmg.moviebooking.service.admin.ScreenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/screens")
@Tag(name = "Admin - Screens", description = "Admin endpoints for screen management")
public class ScreenController {

    private final ScreenService screenService;

    public ScreenController(ScreenService screenService) {
        this.screenService = screenService;
    }

    @PostMapping
    @Operation(summary = "Create a new screen")
    public ResponseEntity<ScreenResponse> createScreen(@Valid @RequestBody ScreenRequest request) {
        ScreenResponse response = screenService.createScreen(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get screens by theater ID")
    public ResponseEntity<List<ScreenResponse>> getScreensByTheater(@RequestParam Long theaterId) {
        return ResponseEntity.ok(screenService.getScreensByTheaterId(theaterId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get screen by ID")
    public ResponseEntity<ScreenResponse> getScreenById(@PathVariable Long id) {
        return ResponseEntity.ok(screenService.getScreenById(id));
    }
}
