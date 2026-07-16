package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.TheaterRequest;
import com.dmg.moviebooking.dto.response.TheaterResponse;
import com.dmg.moviebooking.entity.City;
import com.dmg.moviebooking.entity.Theater;
import com.dmg.moviebooking.exception.DuplicateResourceException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
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
    private final CityService cityService;

    public TheaterService(TheaterRepository theaterRepository, CityService cityService) {
        this.theaterRepository = theaterRepository;
        this.cityService = cityService;
    }

    @CacheEvict(value = "theaters", allEntries = true)
    public TheaterResponse createTheater(TheaterRequest request) {
        City city = cityService.getCityEntity(request.getCityId());

        if (theaterRepository.existsByNameAndCityId(request.getName(), request.getCityId())) {
            throw new DuplicateResourceException(
                    "Theater already exists with name: " + request.getName() + " in city id: " + request.getCityId());
        }

        Theater theater = Theater.builder()
                .name(request.getName())
                .location(request.getLocation())
                .city(city)
                .build();

        theater = theaterRepository.save(theater);
        return toResponse(theater);
    }

    @Cacheable(value = "theaters", key = "#cityId")
    @Transactional(readOnly = true)
    public List<TheaterResponse> getTheatersByCityId(Long cityId) {
        cityService.getCityEntity(cityId); // validate city exists
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
        return TheaterResponse.builder()
                .id(theater.getId())
                .name(theater.getName())
                .location(theater.getLocation())
                .cityId(theater.getCity().getId())
                .cityName(theater.getCity().getName())
                .createdAt(theater.getCreatedAt())
                .updatedAt(theater.getUpdatedAt())
                .build();
    }
}
