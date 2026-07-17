package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.MovieRequest;
import com.dmg.moviebooking.dto.response.MovieResponse;
import com.dmg.moviebooking.entity.Movie;
import com.dmg.moviebooking.exception.DuplicateResourceException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.MovieRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @CacheEvict(value = "movies", allEntries = true)
    public MovieResponse createMovie(MovieRequest request) {
        if (movieRepository.existsByTitle(request.getTitle())) {
            throw new DuplicateResourceException("Movie already exists with title: " + request.getTitle());
        }

        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .genre(request.getGenre())
                .durationMinutes(request.getDurationMinutes())
                .language(request.getLanguage())
                .releaseDate(request.getReleaseDate())
                .posterUrl(request.getPosterUrl())
                .build();

        movie = movieRepository.save(movie);
        return toResponse(movie);
    }

    @CacheEvict(value = "movies", allEntries = true)
    public List<MovieResponse> createMovies(List<MovieRequest> requests) {
        return requests.stream()
                .map(this::createMovie)
                .toList();
    }

    @CacheEvict(value = "movies", allEntries = true)
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));

        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setLanguage(request.getLanguage());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setPosterUrl(request.getPosterUrl());

        movie = movieRepository.save(movie);
        return toResponse(movie);
    }

    @CacheEvict(value = "movies", allEntries = true)
    public void deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new ResourceNotFoundException("Movie", id);
        }
        movieRepository.deleteById(id);
    }

    @Cacheable(value = "movies", key = "'id-' + #id")
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        return toResponse(getMovieEntity(id));
    }

    @Cacheable(value = "movies", key = "'all'")
    @Transactional(readOnly = true)
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> searchMovies(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> searchMoviesByTheater(Long theaterId) {
        return movieRepository.findMoviesByTheaterId(theaterId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Movie getMovieEntity(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }

    private MovieResponse toResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .durationMinutes(movie.getDurationMinutes())
                .language(movie.getLanguage())
                .releaseDate(movie.getReleaseDate())
                .posterUrl(movie.getPosterUrl())
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }
}
