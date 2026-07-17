package com.dmg.moviebooking.controller.admin;

import com.dmg.moviebooking.dto.request.MovieRequest;
import com.dmg.moviebooking.dto.response.MovieResponse;
import com.dmg.moviebooking.service.admin.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/movies")
@Tag(name = "Admin - Movies", description = "Admin endpoints for movie management")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    @Operation(summary = "Create a new movie")
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody MovieRequest request) {
        MovieResponse response = movieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/batch")
    @Operation(summary = "Create multiple movies in a single request")
    public ResponseEntity<List<MovieResponse>> createMovies(@Valid @RequestBody List<MovieRequest> requests) {
        List<MovieResponse> responses = movieService.createMovies(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a movie by ID")
    public ResponseEntity<MovieResponse> updateMovie(@PathVariable Long id,
                                                      @Valid @RequestBody MovieRequest request) {
        MovieResponse response = movieService.updateMovie(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a movie by ID")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all movies")
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get movie by ID")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }
}
