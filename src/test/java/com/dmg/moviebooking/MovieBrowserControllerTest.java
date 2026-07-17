package com.dmg.moviebooking;

import com.dmg.moviebooking.dto.request.MovieRequest;
import com.dmg.moviebooking.dto.request.ShowRequest;
import com.dmg.moviebooking.entity.City;
import com.dmg.moviebooking.entity.Movie;
import com.dmg.moviebooking.entity.Screen;
import com.dmg.moviebooking.entity.Show;
import com.dmg.moviebooking.entity.Theater;
import com.dmg.moviebooking.entity.User;
import com.dmg.moviebooking.enums.Role;
import com.dmg.moviebooking.repository.*;
import com.dmg.moviebooking.security.JwtTokenProvider;
import com.dmg.moviebooking.service.admin.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MovieBrowserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String customerToken;
    private Long testMovieId;
    private Long testTheaterId;
    private Long testShowId;

    @BeforeEach
    void setUp() {
        // Create customer user and token
        if (!userRepository.existsByUsername("movietestuser")) {
            userRepository.save(User.builder()
                    .username("movietestuser")
                    .email("movietest@test.com")
                    .password(passwordEncoder.encode("password"))
                    .fullName("Movie Test User")
                    .role(Role.ROLE_CUSTOMER)
                    .active(true)
                    .build());
        }
        customerToken = jwtTokenProvider.generateToken("movietestuser", List.of("ROLE_CUSTOMER"));

        // Create test city, theater, screen
        City city = cityRepository.save(City.builder().name("MovieBrowser City").build());
        Theater theater = theaterRepository.save(Theater.builder()
                .name("Browser Test Theater")
                .location("Test Location")
                .cityId(city.getId())
                .build());
        testTheaterId = theater.getId();

        Screen screen = screenRepository.save(Screen.builder()
                .name("Browser Test Screen")
                .totalSeats(50)
                .theaterId(theater.getId())
                .build());

        // Create test movie
        Movie movie = movieRepository.save(Movie.builder()
                .title("Browser Test Movie")
                .description("A test movie for browser testing")
                .genre("Test Genre")
                .durationMinutes(120)
                .language("English")
                .build());
        testMovieId = movie.getId();

        // Create a show linking movie to screen
        Show show = showRepository.save(Show.builder()
                .movieId(movie.getId())
                .screenId(screen.getId())
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .basePrice(BigDecimal.valueOf(200))
                .build());
        testShowId = show.getId();

        // Create another movie for more comprehensive tests
        movieRepository.save(Movie.builder()
                .title("Searchable Movie")
                .description("Another movie")
                .genre("Drama")
                .durationMinutes(100)
                .language("French")
                .build());
    }

    // ==================== browseMovies (GET /api/movies) ====================

    @Nested
    class BrowseMovies {

        @Test
        void withoutSearch_ShouldReturnAllMovies() throws Exception {
            mockMvc.perform(get("/api/movies")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                    .andExpect(jsonPath("$[*].title", hasItems("Browser Test Movie", "Searchable Movie")));
        }

        @Test
        void withSearch_ShouldReturnMatchingMovies() throws Exception {
            mockMvc.perform(get("/api/movies?search=Searchable")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title").value("Searchable Movie"));
        }

        @Test
        void withSearch_NoMatch_ShouldReturnEmptyList() throws Exception {
            mockMvc.perform(get("/api/movies?search=NonExistentMovieXYZ")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void withSearch_CaseInsensitive_ShouldReturnMatches() throws Exception {
            mockMvc.perform(get("/api/movies?search=searchable")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title").value("Searchable Movie"));
        }

        @Test
        void withEmptySearch_ShouldReturnAllMovies() throws Exception {
            mockMvc.perform(get("/api/movies?search=")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
        }

        @Test
        void withoutAuth_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/movies"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== getMovieById (GET /api/movies/{id}) ====================

    @Nested
    class GetMovieById {

        @Test
        void shouldReturnMovieDetails() throws Exception {
            mockMvc.perform(get("/api/movies/{id}", testMovieId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testMovieId))
                    .andExpect(jsonPath("$.title").value("Browser Test Movie"))
                    .andExpect(jsonPath("$.description").value("A test movie for browser testing"))
                    .andExpect(jsonPath("$.genre").value("Test Genre"))
                    .andExpect(jsonPath("$.durationMinutes").value(120))
                    .andExpect(jsonPath("$.language").value("English"));
        }

        @Test
        void notFound_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/api/movies/{id}", 99999L)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        void withoutAuth_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/movies/{id}", testMovieId))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== getTheatersByMovie (GET /api/movies/{id}/theaters) ====================

    @Nested
    class GetTheatersByMovie {

        @Test
        void shouldReturnTheatersShowingTheMovie() throws Exception {
            mockMvc.perform(get("/api/movies/{id}/theaters", testMovieId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].name").value("Browser Test Theater"))
                    .andExpect(jsonPath("$[0].location").value("Test Location"));
        }

        @Test
        void movieNotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/api/movies/{id}/theaters", 99999L)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== getShowsByMovie (GET /api/movies/{id}/shows) ====================

    @Nested
    class GetShowsByMovie {

        @Test
        void shouldReturnAllShowsForMovie() throws Exception {
            mockMvc.perform(get("/api/movies/{id}/shows", testMovieId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].movieId").value(testMovieId))
                    .andExpect(jsonPath("$[0].movieTitle").value("Browser Test Movie"))
                    .andExpect(jsonPath("$[0].basePrice").value(200));
        }

        @Test
        void filteredByTheater_ShouldReturnShowsForThatTheater() throws Exception {
            mockMvc.perform(get("/api/movies/{id}/shows?theaterId={theaterId}", testMovieId, testTheaterId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].movieId").value(testMovieId))
                    .andExpect(jsonPath("$[0].theaterName").value("Browser Test Theater"));
        }

        @Test
        void filteredByNonExistentTheater_ShouldReturnEmptyList() throws Exception {
            mockMvc.perform(get("/api/movies/{id}/shows?theaterId={theaterId}", testMovieId, 99999L)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void movieNotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/api/movies/{id}/shows", 99999L)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== getMoviesByTheater (GET /api/theaters/{theaterId}/movies) ====================

    @Nested
    class GetMoviesByTheater {

        @Test
        void shouldReturnMoviesPlayingAtTheater() throws Exception {
            mockMvc.perform(get("/api/theaters/{theaterId}/movies", testTheaterId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[*].title", hasItem("Browser Test Movie")));
        }

        @Test
        void theaterWithNoMovies_ShouldReturnEmptyList() throws Exception {
            // Create a theater with no screens/shows
            City otherCity = cityRepository.save(City.builder().name("Empty City 2").build());
            Theater emptyTheater = theaterRepository.save(Theater.builder()
                    .name("Empty Theater 2")
                    .location("Nowhere")
                    .cityId(otherCity.getId())
                    .build());

            mockMvc.perform(get("/api/theaters/{theaterId}/movies", emptyTheater.getId())
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
