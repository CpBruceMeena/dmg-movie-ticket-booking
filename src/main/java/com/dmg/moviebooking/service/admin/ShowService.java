package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.ShowRequest;
import com.dmg.moviebooking.dto.response.ShowResponse;
import com.dmg.moviebooking.entity.PricingTier;
import com.dmg.moviebooking.entity.Screen;
import com.dmg.moviebooking.entity.Show;
import com.dmg.moviebooking.entity.ShowPricingTier;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.PricingTierRepository;
import com.dmg.moviebooking.repository.ScreenRepository;
import com.dmg.moviebooking.repository.ShowPricingTierRepository;
import com.dmg.moviebooking.repository.ShowRepository;
import com.dmg.moviebooking.repository.TheaterRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ShowService(ShowRepository showRepository,
                       ScreenRepository screenRepository,
                       TheaterRepository theaterRepository,
                       PricingTierRepository pricingTierRepository,
                       ShowPricingTierRepository showPricingTierRepository) {
        this.showRepository = showRepository;
        this.screenRepository = screenRepository;
        this.theaterRepository = theaterRepository;
        this.pricingTierRepository = pricingTierRepository;
        this.showPricingTierRepository = showPricingTierRepository;
    }

    @CacheEvict(value = "shows", allEntries = true)
    public ShowResponse createShow(ShowRequest request) {
        // Validate screen exists
        if (!screenRepository.existsById(request.getScreenId())) {
            throw new ResourceNotFoundException("Screen", request.getScreenId());
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Show show = Show.builder()
                .movieTitle(request.getMovieTitle())
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
        return showRepository.findAll().stream()
                .filter(s -> screenIds.contains(s.getScreenId()))
                .map(this::toResponse)
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

    private ShowResponse toResponse(Show show) {
        String screenName = screenRepository.findById(show.getScreenId())
                .map(Screen::getName)
                .orElse("Unknown");

        // Look up theater name via screen
        String theaterName = screenRepository.findById(show.getScreenId())
                .flatMap(screen -> theaterRepository.findById(screen.getTheaterId()))
                .map(theater -> theater.getName())
                .orElse("Unknown");

        Set<Long> pricingTierIds = showPricingTierRepository.findByShowId(show.getId())
                .stream()
                .map(ShowPricingTier::getPricingTierId)
                .collect(Collectors.toSet());

        return ShowResponse.builder()
                .id(show.getId())
                .movieTitle(show.getMovieTitle())
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
