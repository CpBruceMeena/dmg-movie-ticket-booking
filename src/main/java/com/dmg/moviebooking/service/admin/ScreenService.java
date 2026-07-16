package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.ScreenRequest;
import com.dmg.moviebooking.dto.response.ScreenResponse;
import com.dmg.moviebooking.entity.Screen;
import com.dmg.moviebooking.entity.Theater;
import com.dmg.moviebooking.exception.DuplicateResourceException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.ScreenRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final TheaterService theaterService;

    public ScreenService(ScreenRepository screenRepository, TheaterService theaterService) {
        this.screenRepository = screenRepository;
        this.theaterService = theaterService;
    }

    @CacheEvict(value = "screens", allEntries = true)
    public ScreenResponse createScreen(ScreenRequest request) {
        Theater theater = theaterService.getTheaterEntity(request.getTheaterId());

        if (screenRepository.existsByNameAndTheaterId(request.getName(), request.getTheaterId())) {
            throw new DuplicateResourceException(
                    "Screen already exists with name: " + request.getName() + " in theater id: " + request.getTheaterId());
        }

        Screen screen = Screen.builder()
                .name(request.getName())
                .totalSeats(request.getTotalSeats())
                .theater(theater)
                .build();

        screen = screenRepository.save(screen);
        return toResponse(screen);
    }

    @Cacheable(value = "screens", key = "#theaterId")
    @Transactional(readOnly = true)
    public List<ScreenResponse> getScreensByTheaterId(Long theaterId) {
        theaterService.getTheaterEntity(theaterId); // validate theater exists
        return screenRepository.findByTheaterId(theaterId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "screens", key = "'id-' + #id")
    @Transactional(readOnly = true)
    public ScreenResponse getScreenById(Long id) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", id));
        return toResponse(screen);
    }

    public Screen getScreenEntity(Long id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", id));
    }

    private ScreenResponse toResponse(Screen screen) {
        return ScreenResponse.builder()
                .id(screen.getId())
                .name(screen.getName())
                .totalSeats(screen.getTotalSeats())
                .theaterId(screen.getTheater().getId())
                .theaterName(screen.getTheater().getName())
                .createdAt(screen.getCreatedAt())
                .updatedAt(screen.getUpdatedAt())
                .build();
    }
}
