package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.CityRequest;
import com.dmg.moviebooking.dto.response.CityResponse;
import com.dmg.moviebooking.entity.City;
import com.dmg.moviebooking.exception.DuplicateResourceException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public CityResponse createCity(CityRequest request) {
        if (cityRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("City already exists with name: " + request.getName());
        }

        City city = City.builder()
                .name(request.getName())
                .build();

        city = cityRepository.save(city);
        return toResponse(city);
    }

    @Transactional(readOnly = true)
    public List<CityResponse> getAllCities() {
        return cityRepository.findAllByOrderByName().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CityResponse getCityById(Long id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City", id));
        return toResponse(city);
    }

    public CityResponse updateCity(Long id, CityRequest request) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City", id));

        if (!city.getName().equalsIgnoreCase(request.getName())
                && cityRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("City already exists with name: " + request.getName());
        }

        city.setName(request.getName());
        city = cityRepository.save(city);
        return toResponse(city);
    }

    public void deleteCity(Long id) {
        if (!cityRepository.existsById(id)) {
            throw new ResourceNotFoundException("City", id);
        }
        cityRepository.deleteById(id);
    }

    private CityResponse toResponse(City city) {
        return CityResponse.builder()
                .id(city.getId())
                .name(city.getName())
                .createdAt(city.getCreatedAt())
                .updatedAt(city.getUpdatedAt())
                .build();
    }

    public City getCityEntity(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City", id));
    }
}
