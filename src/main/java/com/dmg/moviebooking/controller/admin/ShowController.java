package com.dmg.moviebooking.controller.admin;

import com.dmg.moviebooking.dto.request.ShowRequest;
import com.dmg.moviebooking.dto.response.ShowResponse;
import com.dmg.moviebooking.service.admin.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shows")
@Tag(name = "Admin - Shows", description = "Admin endpoints for show management")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    @Operation(summary = "Create a new show")
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody ShowRequest request) {
        ShowResponse response = showService.createShow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get shows by screen or theater ID")
    public ResponseEntity<List<ShowResponse>> getShows(
            @RequestParam(required = false) Long screenId,
            @RequestParam(required = false) Long theaterId) {
        if (theaterId != null) {
            return ResponseEntity.ok(showService.getShowsByTheaterId(theaterId));
        }
        if (screenId != null) {
            return ResponseEntity.ok(showService.getShowsByScreenId(screenId));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get show by ID")
    public ResponseEntity<ShowResponse> getShowById(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getShowById(id));
    }
}
