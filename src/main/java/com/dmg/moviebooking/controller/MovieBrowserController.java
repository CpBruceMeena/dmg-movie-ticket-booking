package com.dmg.moviebooking.controller;

import com.dmg.moviebooking.dto.response.MovieResponse;
import com.dmg.moviebooking.dto.response.TheaterResponse;
import com.dmg.moviebooking.service.admin.MovieService;
import com.dmg.moviebooking.service.admin.TheaterService;
import com.dmg.moviebooking.service.admin.ShowService;
import com.dmg.moviebooking.dto.response.ShowResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Customer - Browse", description = "Customer endpoints for browsing movies, theaters, and shows")
public class MovieBrowserController {

    private final MovieService movieService;
    private final TheaterService theaterService;
    private final ShowService showService;

    public MovieBrowserController(MovieService movieService,
                                  TheaterService theaterService,
                                  ShowService showService) {
        this.movieService = movieService;
        this.theaterService = theaterService;
        this.showService = showService;
    }

    // ==================== Movie browsing ====================

    @GetMapping("/movies")
    @Operation(summary = "Browse all movies or search by title")
    public ResponseEntity<List<MovieResponse>> browseMovies(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(movieService.searchMovies(search));
        }
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/movies/{id}")
    @Operation(summary = "Get movie details by ID")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    // ==================== Theaters showing a movie ====================

    @GetMapping("/movies/{id}/theaters")
    @Operation(summary = "Get all theaters showing a specific movie")
    public ResponseEntity<List<TheaterResponse>> getTheatersByMovie(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getTheatersByMovieId(id));
    }

    // ==================== Shows for a movie ====================

    @GetMapping("/movies/{id}/shows")
    @Operation(summary = "Get all shows for a movie (optionally filtered by theater)")
    public ResponseEntity<List<ShowResponse>> getShowsByMovie(
            @PathVariable Long id,
            @RequestParam(required = false) Long theaterId) {
        return ResponseEntity.ok(showService.getShowsByMovieId(id, theaterId));
    }

    // ==================== Movies in a theater ====================

    @GetMapping("/theaters/{theaterId}/movies")
    @Operation(summary = "Get all movies playing in a specific theater")
    public ResponseEntity<List<MovieResponse>> getMoviesByTheater(@PathVariable Long theaterId) {
        return ResponseEntity.ok(movieService.searchMoviesByTheater(theaterId));
    }
}
