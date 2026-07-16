package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.TheaterRequest;
import com.dmg.moviebooking.dto.response.TheaterResponse;
import com.dmg.moviebooking.entity.City;
import com.dmg.moviebooking.entity.Theater;
import com.dmg.moviebooking.exception.DuplicateResourceException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.CityRepository;
import com.dmg.moviebooking.repository.TheaterRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TheaterService {

    private final TheaterRepository theaterRepository;
    private final CityRepository cityRepository;

    public TheaterService(TheaterRepository theaterRepository, CityRepository cityRepository) {
        this.theaterRepository = theaterRepository;
        this.cityRepository = cityRepository;
    }

    @CacheEvict(value = "theaters", allEntries = true)
    public TheaterResponse createTheater(TheaterRequest request) {
        // Validate city exists
        if (!cityRepository.existsById(request.getCityId())) {
            throw new ResourceNotFoundException("City", request.getCityId());
        }

        if (theaterRepository.existsByNameAndCityId(request.getName(), request.getCityId())) {
            throw new DuplicateResourceException(
                    "Theater already exists with name: " + request.getName() + " in city id: " + request.getCityId());
        }

        Theater theater = Theater.builder()
                .name(request.getName())
                .location(request.getLocation())
                .cityId(request.getCityId())
                .build();

        theater = theaterRepository.save(theater);
        return toResponse(theater);
    }

    @Cacheable(value = "theaters", key = "#cityId")
    @Transactional(readOnly = true)
    public List<TheaterResponse> getTheatersByCityId(Long cityId) {
        return theaterRepository.findByCityId(cityId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "theaters", key = "'id-' + #id")
    @Transactional(readOnly = true)
    public TheaterResponse getTheaterById(Long id) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater", id));
        return toResponse(theater);
    }

    public Theater getTheaterEntity(Long id) {
        return theaterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater", id));
    }

    private TheaterResponse toResponse(Theater theater) {
        String cityName = cityRepository.findById(theater.getCityId())
                .map(City::getName)
                .orElse("Unknown");

        return TheaterResponse.builder()
                .id(theater.getId())
                .name(theater.getName())
                .location(theater.getLocation())
                .cityId(theater.getCityId())
                .cityName(cityName)
                .createdAt(theater.getCreatedAt())
                .updatedAt(theater.getUpdatedAt())
                .build();
    }
}
