package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.PricingTierRequest;
import com.dmg.moviebooking.dto.response.PricingTierResponse;
import com.dmg.moviebooking.entity.PricingTier;
import com.dmg.moviebooking.exception.DuplicateResourceException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.PricingTierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PricingService {

    private final PricingTierRepository pricingTierRepository;

    public PricingService(PricingTierRepository pricingTierRepository) {
        this.pricingTierRepository = pricingTierRepository;
    }

    public PricingTierResponse createPricingTier(PricingTierRequest request) {
        if (pricingTierRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Pricing tier already exists with name: " + request.getName());
        }

        PricingTier tier = PricingTier.builder()
                .name(request.getName())
                .seatType(request.getSeatType())
                .weekdayPrice(request.getWeekdayPrice())
                .weekendPrice(request.getWeekendPrice())
                .build();

        tier = pricingTierRepository.save(tier);
        return toResponse(tier);
    }

    @Transactional(readOnly = true)
    public List<PricingTierResponse> getAllPricingTiers() {
        return pricingTierRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public PricingTierResponse updatePricingTier(Long id, PricingTierRequest request) {
        PricingTier tier = pricingTierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PricingTier", id));

        tier.setName(request.getName());
        tier.setSeatType(request.getSeatType());
        tier.setWeekdayPrice(request.getWeekdayPrice());
        tier.setWeekendPrice(request.getWeekendPrice());

        tier = pricingTierRepository.save(tier);
        return toResponse(tier);
    }

    private PricingTierResponse toResponse(PricingTier tier) {
        return PricingTierResponse.builder()
                .id(tier.getId())
                .name(tier.getName())
                .seatType(tier.getSeatType())
                .weekdayPrice(tier.getWeekdayPrice())
                .weekendPrice(tier.getWeekendPrice())
                .createdAt(tier.getCreatedAt())
                .updatedAt(tier.getUpdatedAt())
                .build();
    }
}
