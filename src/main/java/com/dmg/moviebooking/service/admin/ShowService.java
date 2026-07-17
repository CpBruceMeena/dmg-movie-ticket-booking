package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.ShowRequest;
import com.dmg.moviebooking.dto.response.ShowResponse;
import com.dmg.moviebooking.dto.response.TheaterResponse;
import com.dmg.moviebooking.entity.City;
import com.dmg.moviebooking.entity.Movie;
import com.dmg.moviebooking.entity.Screen;
import com.dmg.moviebooking.entity.Show;
import com.dmg.moviebooking.entity.ShowPricingTier;
import com.dmg.moviebooking.entity.Theater;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShowService {

    private final ShowRepository showRepository;
    private final ScreenRepository screenRepository;
    private final TheaterRepository theaterRepository;
    private final PricingTierRepository pricingTierRepository;
    private final ShowPricingTierRepository showPricingTierRepository;
    private final MovieRepository movieRepository;
    private final CityRepository cityRepository;

    public ShowService(ShowRepository showRepository,
                       ScreenRepository screenRepository,
                       TheaterRepository theaterRepository,
                       PricingTierRepository pricingTierRepository,
                       ShowPricingTierRepository showPricingTierRepository,
                       MovieRepository movieRepository,
                       CityRepository cityRepository) {
        this.showRepository = showRepository;
        this.screenRepository = screenRepository;
        this.theaterRepository = theaterRepository;
        this.pricingTierRepository = pricingTierRepository;
        this.showPricingTierRepository = showPricingTierRepository;
        this.movieRepository = movieRepository;
        this.cityRepository = cityRepository;
    }

    @CacheEvict(value = "shows", allEntries = true)
    public ShowResponse createShow(ShowRequest request) {
        // Validate screen exists
        if (!screenRepository.existsById(request.getScreenId())) {
            throw new ResourceNotFoundException("Screen", request.getScreenId());
        }

        // Validate movie exists
        if (!movieRepository.existsById(request.getMovieId())) {
            throw new ResourceNotFoundException("Movie", request.getMovieId());
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Show show = Show.builder()
                .movieId(request.getMovieId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .basePrice(request.getBasePrice())
                .screenId(request.getScreenId())
                .build();

        show = showRepository.save(show);

        // Save pricing tier associations as ShowPricingTier records
        if (request.getPricingTierIds() != null && !request.getPricingTierIds().isEmpty()) {
            for (Long tierId : request.getPricingTierIds()) {
                ShowPricingTier spt = ShowPricingTier.builder()
                        .showId(show.getId())
                        .pricingTierId(tierId)
                        .build();
                showPricingTierRepository.save(spt);
            }
        }

        return toResponse(show);
    }

    @CacheEvict(value = "shows", allEntries = true)
    public List<ShowResponse> createShows(List<ShowRequest> requests) {
        return requests.stream()
                .map(this::createShow)
                .toList();
    }

    @Cacheable(value = "shows", key = "'screen-' + #screenId")
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByScreenId(Long screenId) {
        return showRepository.findByScreenId(screenId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByTheaterId(Long theaterId) {
        List<Long> screenIds = screenRepository.findByTheaterId(theaterId).stream()
                .map(Screen::getId)
                .toList();
        if (screenIds.isEmpty()) {
            return List.of();
        }
        return showRepository.findByScreenIds(screenIds).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByMovieId(Long movieId, Long theaterId) {
        // Validate movie exists
        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie", movieId);
        }

        List<Show> shows;
        if (theaterId != null) {
            // Filter by both movie and theater
            List<Long> screenIds = screenRepository.findByTheaterId(theaterId).stream()
                    .map(Screen::getId)
                    .toList();
            if (screenIds.isEmpty()) {
                return List.of();
            }
            shows = showRepository.findByScreenIds(screenIds).stream()
                    .filter(s -> s.getMovieId().equals(movieId))
                    .toList();
        } else {
            shows = showRepository.findByMovieId(movieId);
        }

        return shows.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TheaterResponse> getTheatersByMovieId(Long movieId) {
        // Validate movie exists
        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie", movieId);
        }

        List<Long> screenIds = showRepository.findScreenIdsByMovieId(movieId);
        if (screenIds.isEmpty()) {
            return List.of();
        }

        // Get distinct theater IDs from screens
        List<Long> theaterIds = screenRepository.findAllById(screenIds).stream()
                .map(Screen::getTheaterId)
                .distinct()
                .toList();

        return theaterRepository.findAllById(theaterIds).stream()
                .map(theater -> {
                    String cityName = cityRepository.findById(theater.getCityId())
                            .map(City::getName)
                            .orElse("Unknown");
                    return TheaterResponse.builder()
                            .id(theater.getId())
                            .name(theater.getName())
                            .location(theater.getLocation())
                            .cityId(theater.getCityId())
                            .createdAt(theater.getCreatedAt())
                            .updatedAt(theater.getUpdatedAt())
                            .build();
                })
                .toList();
    }

    @Cacheable(value = "shows", key = "'id-' + #id")
    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long id) {
        return toResponse(getShowEntity(id));
    }

    @Cacheable(value = "shows", key = "'all'")
    @Transactional(readOnly = true)
    public List<ShowResponse> getAllShows() {
        return showRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Show getShowEntity(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));
    }

    /**
     * Find shows starting within a given time window.
     * Used by the notification reminder scheduler to find upcoming shows.
     */
    @Transactional(readOnly = true)
    public List<Show> getShowsStartingBetween(LocalDateTime start, LocalDateTime end) {
        return showRepository.findByStartTimeBetween(start, end);
    }

    private ShowResponse toResponse(Show show) {
        String screenName = screenRepository.findById(show.getScreenId())
                .map(Screen::getName)
                .orElse("Unknown");

        // Look up theater name via screen
        String theaterName = screenRepository.findById(show.getScreenId())
                .flatMap(screen -> theaterRepository.findById(screen.getTheaterId()))
                .map(Theater::getName)
                .orElse("Unknown");

        // Look up movie details
        String movieTitle = "Unknown";
        String movieGenre = null;
        Integer movieDurationMinutes = null;
        if (show.getMovieId() != null) {
            Movie movie = movieRepository.findById(show.getMovieId()).orElse(null);
            if (movie != null) {
                movieTitle = movie.getTitle();
                movieGenre = movie.getGenre();
                movieDurationMinutes = movie.getDurationMinutes();
            }
        }

        Set<Long> pricingTierIds = showPricingTierRepository.findByShowId(show.getId())
                .stream()
                .map(ShowPricingTier::getPricingTierId)
                .collect(Collectors.toSet());

        return ShowResponse.builder()
                .id(show.getId())
                .movieId(show.getMovieId())
                .movieTitle(movieTitle)
                .movieGenre(movieGenre)
                .movieDurationMinutes(movieDurationMinutes)
                .startTime(show.getStartTime())
                .endTime(show.getEndTime())
                .basePrice(show.getBasePrice())
                .screenId(show.getScreenId())
                .screenName(screenName)
                .theaterName(theaterName)
                .pricingTierIds(pricingTierIds)
                .createdAt(show.getCreatedAt())
                .updatedAt(show.getUpdatedAt())
                .build();
    }
}
