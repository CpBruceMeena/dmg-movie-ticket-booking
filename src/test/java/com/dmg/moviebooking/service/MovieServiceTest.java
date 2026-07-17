package com.dmg.moviebooking.service;

import com.dmg.moviebooking.dto.request.MovieRequest;
import com.dmg.moviebooking.dto.response.MovieResponse;
import com.dmg.moviebooking.entity.City;
import com.dmg.moviebooking.entity.Movie;
import com.dmg.moviebooking.entity.Screen;
import com.dmg.moviebooking.entity.Show;
import com.dmg.moviebooking.entity.Theater;
import com.dmg.moviebooking.exception.DuplicateResourceException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.CityRepository;
import com.dmg.moviebooking.repository.MovieRepository;
import com.dmg.moviebooking.repository.ScreenRepository;
import com.dmg.moviebooking.repository.ShowRepository;
import com.dmg.moviebooking.repository.TheaterRepository;
import com.dmg.moviebooking.service.admin.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MovieServiceTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private ShowRepository showRepository;

    private Long theaterId;

    @BeforeEach
    void setUp() {
        // Create a theater with a show for searchMoviesByTheater tests
        City city = cityRepository.save(City.builder().name("MovieTest City").build());
        Theater theater = theaterRepository.save(Theater.builder()
                .name("MovieTest Theater")
                .location("Test Location")
                .cityId(city.getId())
                .build());
        theaterId = theater.getId();

        Screen screen = screenRepository.save(Screen.builder()
                .name("MovieTest Screen")
                .totalSeats(50)
                .theaterId(theaterId)
                .build());

        // Create a movie that will be linked to a show for the theater
        Movie linkedMovie = movieRepository.save(Movie.builder()
                .title("Theater Linked Movie")
                .description("A movie playing at the test theater")
                .genre("Action")
                .durationMinutes(120)
                .language("English")
                .build());

        showRepository.save(Show.builder()
                .movieId(linkedMovie.getId())
                .screenId(screen.getId())
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(2))
                .basePrice(BigDecimal.valueOf(250))
                .build());
    }

    // ==================== createMovie ====================

    @Test
    void createMovie_ShouldCreateMovieSuccessfully() {
        MovieRequest request = MovieRequest.builder()
                .title("Inception")
                .description("A mind-bending thriller")
                .genre("Sci-Fi")
                .durationMinutes(148)
                .language("English")
                .releaseDate(LocalDate.of(2010, 7, 16))
                .posterUrl("https://example.com/inception.jpg")
                .build();

        MovieResponse response = movieService.createMovie(request);

        assertNotNull(response.getId());
        assertEquals("Inception", response.getTitle());
        assertEquals("A mind-bending thriller", response.getDescription());
        assertEquals("Sci-Fi", response.getGenre());
        assertEquals(148, response.getDurationMinutes());
        assertEquals("English", response.getLanguage());
        assertEquals(LocalDate.of(2010, 7, 16), response.getReleaseDate());
        assertEquals("https://example.com/inception.jpg", response.getPosterUrl());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void createMovie_ShouldCreateMovieWithMinimalFields() {
        MovieRequest request = MovieRequest.builder()
                .title("Minimal Movie")
                .durationMinutes(90)
                .build();

        MovieResponse response = movieService.createMovie(request);

        assertNotNull(response.getId());
        assertEquals("Minimal Movie", response.getTitle());
        assertNull(response.getDescription());
        assertNull(response.getGenre());
        assertEquals(90, response.getDurationMinutes());
        assertNull(response.getLanguage());
        assertNull(response.getReleaseDate());
        assertNull(response.getPosterUrl());
    }

    @Test
    void createMovie_WithDuplicateTitle_ShouldThrowDuplicateResourceException() {
        MovieRequest request = MovieRequest.builder()
                .title("Unique Movie")
                .durationMinutes(100)
                .build();
        movieService.createMovie(request);

        MovieRequest duplicateRequest = MovieRequest.builder()
                .title("Unique Movie")
                .durationMinutes(120)
                .build();

        assertThrows(DuplicateResourceException.class,
                () -> movieService.createMovie(duplicateRequest));
    }

    // ==================== createMovies (Batch) ====================

    @Test
    void createMovies_Batch_ShouldCreateAllMovies() {
        MovieRequest movie1 = MovieRequest.builder()
                .title("Movie One")
                .durationMinutes(100)
                .build();
        MovieRequest movie2 = MovieRequest.builder()
                .title("Movie Two")
                .durationMinutes(110)
                .build();
        MovieRequest movie3 = MovieRequest.builder()
                .title("Movie Three")
                .durationMinutes(120)
                .build();

        List<MovieResponse> responses = movieService.createMovies(List.of(movie1, movie2, movie3));

        assertEquals(3, responses.size());
        assertEquals("Movie One", responses.get(0).getTitle());
        assertEquals("Movie Two", responses.get(1).getTitle());
        assertEquals("Movie Three", responses.get(2).getTitle());
        assertNotNull(responses.get(0).getId());
        assertNotNull(responses.get(1).getId());
        assertNotNull(responses.get(2).getId());
    }

    @Test
    void createMovies_Batch_WithDuplicateInBatch_ShouldThrowAndRollback() {
        MovieRequest movie1 = MovieRequest.builder()
                .title("Batch Movie")
                .durationMinutes(100)
                .build();

        assertThrows(DuplicateResourceException.class,
                () -> movieService.createMovies(List.of(movie1, movie1)));
    }

    @Test
    void createMovies_Batch_EmptyList_ShouldReturnEmptyList() {
        List<MovieResponse> responses = movieService.createMovies(List.of());
        assertTrue(responses.isEmpty());
    }

    // ==================== getMovieById ====================

    @Test
    void getMovieById_ShouldReturnMovie() {
        MovieRequest request = MovieRequest.builder()
                .title("Findable Movie")
                .durationMinutes(95)
                .build();
        MovieResponse created = movieService.createMovie(request);

        MovieResponse found = movieService.getMovieById(created.getId());

        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
        assertEquals("Findable Movie", found.getTitle());
    }

    @Test
    void getMovieById_NotFound_ShouldThrowResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class,
                () -> movieService.getMovieById(99999L));
    }

    // ==================== getAllMovies ====================

    @Test
    void getAllMovies_ShouldReturnAllMovies() {
        movieService.createMovie(MovieRequest.builder().title("List Movie 1").durationMinutes(90).build());
        movieService.createMovie(MovieRequest.builder().title("List Movie 2").durationMinutes(100).build());

        List<MovieResponse> allMovies = movieService.getAllMovies();

        // At least 3: the 2 above + the one from setUp
        assertTrue(allMovies.size() >= 2);
        assertTrue(allMovies.stream().anyMatch(m -> m.getTitle().equals("List Movie 1")));
        assertTrue(allMovies.stream().anyMatch(m -> m.getTitle().equals("List Movie 2")));
    }

    // ==================== updateMovie ====================

    @Test
    void updateMovie_ShouldUpdateAllFields() {
        MovieRequest createRequest = MovieRequest.builder()
                .title("Original Title")
                .description("Original description")
                .genre("Drama")
                .durationMinutes(100)
                .language("Hindi")
                .build();
        MovieResponse created = movieService.createMovie(createRequest);

        MovieRequest updateRequest = MovieRequest.builder()
                .title("Updated Title")
                .description("Updated description")
                .genre("Comedy")
                .durationMinutes(120)
                .language("English")
                .releaseDate(LocalDate.of(2024, 1, 1))
                .posterUrl("https://example.com/poster.jpg")
                .build();

        MovieResponse updated = movieService.updateMovie(created.getId(), updateRequest);

        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated description", updated.getDescription());
        assertEquals("Comedy", updated.getGenre());
        assertEquals(120, updated.getDurationMinutes());
        assertEquals("English", updated.getLanguage());
        assertEquals(LocalDate.of(2024, 1, 1), updated.getReleaseDate());
        assertEquals("https://example.com/poster.jpg", updated.getPosterUrl());
    }

    @Test
    void updateMovie_NotFound_ShouldThrowResourceNotFoundException() {
        MovieRequest request = MovieRequest.builder()
                .title("Ghost Movie")
                .durationMinutes(90)
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> movieService.updateMovie(99999L, request));
    }

    // ==================== deleteMovie ====================

    @Test
    void deleteMovie_ShouldRemoveMovie() {
        MovieRequest request = MovieRequest.builder()
                .title("Delete Me")
                .durationMinutes(80)
                .build();
        MovieResponse created = movieService.createMovie(request);

        movieService.deleteMovie(created.getId());

        assertThrows(ResourceNotFoundException.class,
                () -> movieService.getMovieById(created.getId()));
        assertFalse(movieRepository.existsById(created.getId()));
    }

    @Test
    void deleteMovie_NotFound_ShouldThrowResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class,
                () -> movieService.deleteMovie(99999L));
    }

    // ==================== searchMovies ====================

    @Test
    void searchMovies_ByExactTitle_ShouldReturnMatchingMovies() {
        movieService.createMovie(MovieRequest.builder().title("The Dark Knight").durationMinutes(152).build());
        movieService.createMovie(MovieRequest.builder().title("The Dark Knight Rises").durationMinutes(165).build());
        movieService.createMovie(MovieRequest.builder().title("Interstellar").durationMinutes(169).build());

        List<MovieResponse> results = movieService.searchMovies("The Dark Knight");

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(m -> m.getTitle().contains("The Dark Knight")));
    }

    @Test
    void searchMovies_ByPartialTitle_ShouldReturnMatches() {
        movieService.createMovie(MovieRequest.builder().title("Avengers: Endgame").durationMinutes(181).build());
        movieService.createMovie(MovieRequest.builder().title("Avengers: Infinity War").durationMinutes(149).build());
        movieService.createMovie(MovieRequest.builder().title("The Avengers").durationMinutes(143).build());

        List<MovieResponse> results = movieService.searchMovies("Avengers");

        assertEquals(3, results.size());
    }

    @Test
    void searchMovies_CaseInsensitive_ShouldReturnMatches() {
        movieService.createMovie(MovieRequest.builder().title("TENET").durationMinutes(150).build());

        List<MovieResponse> resultsLower = movieService.searchMovies("tenet");
        List<MovieResponse> resultsMixed = movieService.searchMovies("TeneT");
        List<MovieResponse> resultsUpper = movieService.searchMovies("TENET");

        assertEquals(1, resultsLower.size());
        assertEquals(1, resultsMixed.size());
        assertEquals(1, resultsUpper.size());
    }

    @Test
    void searchMovies_NoMatch_ShouldReturnEmptyList() {
        List<MovieResponse> results = movieService.searchMovies("NonExistentMovieXYZ");
        assertTrue(results.isEmpty());
    }

    @Test
    void searchMovies_EmptyQuery_ShouldReturnAllMovies() {
        // An empty LIKE '%%' query matches everything — JPA translates
        // findByTitleContainingIgnoreCase("") to WHERE title LIKE '%%'
        List<MovieResponse> results = movieService.searchMovies("");
        assertFalse(results.isEmpty());
    }

    // ==================== searchMoviesByTheater ====================

    @Test
    void searchMoviesByTheater_ShouldReturnMoviesPlayingAtTheater() {
        // The setUp method created a movie "Theater Linked Movie" with a show at the test theater
        List<MovieResponse> movies = movieService.searchMoviesByTheater(theaterId);

        assertFalse(movies.isEmpty());
        assertTrue(movies.stream().anyMatch(m -> m.getTitle().equals("Theater Linked Movie")));
    }

    @Test
    void searchMoviesByTheater_NoShows_ShouldReturnEmptyList() {
        // A theater ID with no shows should return empty
        City otherCity = cityRepository.save(City.builder().name("Empty City").build());
        Theater emptyTheater = theaterRepository.save(Theater.builder()
                .name("Empty Theater")
                .location("Nowhere")
                .cityId(otherCity.getId())
                .build());

        List<MovieResponse> movies = movieService.searchMoviesByTheater(emptyTheater.getId());
        assertTrue(movies.isEmpty());
    }

    @Test
    void searchMoviesByTheater_InvalidTheaterId_ShouldReturnEmptyList() {
        List<MovieResponse> movies = movieService.searchMoviesByTheater(99999L);
        assertTrue(movies.isEmpty());
    }
}
