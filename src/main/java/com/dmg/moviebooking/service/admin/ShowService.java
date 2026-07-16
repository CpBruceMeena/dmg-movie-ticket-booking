package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.ShowRequest;
import com.dmg.moviebooking.dto.response.ShowResponse;
import com.dmg.moviebooking.entity.PricingTier;
import com.dmg.moviebooking.entity.Screen;
import com.dmg.moviebooking.entity.Show;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.PricingTierRepository;
import com.dmg.moviebooking.repository.ShowRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShowService {

    private final ShowRepository showRepository;
    private final ScreenService screenService;
    private final PricingTierRepository pricingTierRepository;

    public ShowService(ShowRepository showRepository, ScreenService screenService,
                       PricingTierRepository pricingTierRepository) {
        this.showRepository = showRepository;
        this.screenService = screenService;
        this.pricingTierRepository = pricingTierRepository;
    }

    @CacheEvict(value = "shows", allEntries = true)
    public ShowResponse createShow(ShowRequest request) {
        Screen screen = screenService.getScreenEntity(request.getScreenId());

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Show show = Show.builder()
                .movieTitle(request.getMovieTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .basePrice(request.getBasePrice())
                .screen(screen)
                .build();

        if (request.getPricingTierIds() != null && !request.getPricingTierIds().isEmpty()) {
            Set<PricingTier> tiers = new HashSet<>(pricingTierRepository.findAllById(request.getPricingTierIds()));
            show.setPricingTiers(tiers);
        }

        show = showRepository.save(show);
        return toResponse(show);
    }

    @Cacheable(value = "shows", key = "'screen-' + #screenId")
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByScreenId(Long screenId) {
        screenService.getScreenEntity(screenId); // validate screen exists
        return showRepository.findByScreenId(screenId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "shows", key = "'theater-' + #theaterId")
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByTheaterId(Long theaterId) {
        return showRepository.findByTheaterId(theaterId).stream()
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
        return ShowResponse.builder()
                .id(show.getId())
                .movieTitle(show.getMovieTitle())
                .startTime(show.getStartTime())
                .endTime(show.getEndTime())
                .basePrice(show.getBasePrice())
                .screenId(show.getScreen().getId())
                .screenName(show.getScreen().getName())
                .theaterName(show.getScreen().getTheater().getName())
                .pricingTierIds(show.getPricingTiers().stream().map(PricingTier::getId).collect(Collectors.toSet()))
                .createdAt(show.getCreatedAt())
                .updatedAt(show.getUpdatedAt())
                .build();
    }
}
