package com.dmg.moviebooking.controller.admin;

import com.dmg.moviebooking.dto.request.TheaterRequest;
import com.dmg.moviebooking.dto.response.TheaterResponse;
import com.dmg.moviebooking.service.admin.TheaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/theaters")
@Tag(name = "Admin - Theaters", description = "Admin endpoints for theater management")
public class TheaterController {

    private final TheaterService theaterService;

    public TheaterController(TheaterService theaterService) {
        this.theaterService = theaterService;
    }

    @PostMapping
    @Operation(summary = "Create a new theater")
    public ResponseEntity<TheaterResponse> createTheater(@Valid @RequestBody TheaterRequest request) {
        TheaterResponse response = theaterService.createTheater(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get theaters by city ID")
    public ResponseEntity<List<TheaterResponse>> getTheatersByCity(@RequestParam Long cityId) {
        return ResponseEntity.ok(theaterService.getTheatersByCityId(cityId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get theater by ID")
    public ResponseEntity<TheaterResponse> getTheaterById(@PathVariable Long id) {
        return ResponseEntity.ok(theaterService.getTheaterById(id));
    }
}
