package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.DiscountCodeRequest;
import com.dmg.moviebooking.dto.response.DiscountCodeResponse;
import com.dmg.moviebooking.entity.DiscountCode;
import com.dmg.moviebooking.exception.DuplicateResourceException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.DiscountCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DiscountCodeService {

    private static final Logger log = LoggerFactory.getLogger(DiscountCodeService.class);

    private final DiscountCodeRepository discountCodeRepository;

    public DiscountCodeService(DiscountCodeRepository discountCodeRepository) {
        this.discountCodeRepository = discountCodeRepository;
    }

    @CacheEvict(value = "discountCodes", allEntries = true)
    public DiscountCodeResponse createDiscountCode(DiscountCodeRequest request) {
        String code = request.getCode().toUpperCase().trim();
        if (discountCodeRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Discount code already exists: " + code);
        }

        DiscountCode discountCode = DiscountCode.builder()
                .code(code)
                .discountAmount(request.getDiscountAmount())
                .active(true)
                .used(false)
                .expiresAt(request.getExpiresAt())
                .description(request.getDescription())
                .build();

        discountCode = discountCodeRepository.save(discountCode);
        log.info("Discount code '{}' created (amount: ₹{})", code, request.getDiscountAmount());
        return toResponse(discountCode);
    }

    @Cacheable(value = "discountCodes")
    @Transactional(readOnly = true)
    public List<DiscountCodeResponse> getAllDiscountCodes() {
        return discountCodeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DiscountCodeResponse getDiscountCodeById(Long id) {
        DiscountCode code = discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DiscountCode", id));
        return toResponse(code);
    }

    /**
     * Validate a discount code by its string value.
     * Checks that the code exists, is active, not used, and not expired.
     *
     * @param code the discount code string
     * @return the validated DiscountCode entity
     * @throws ResourceNotFoundException if code doesn't exist or is invalid
     */
    @Transactional(readOnly = true)
    public DiscountCode validateAndGetCode(String code) {
        String normalizedCode = code.toUpperCase().trim();
        DiscountCode discountCode = discountCodeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid discount code: " + code));

        if (!discountCode.isActive()) {
            throw new ResourceNotFoundException("Discount code '" + code + "' is no longer active");
        }

        if (discountCode.isUsed()) {
            throw new ResourceNotFoundException("Discount code '" + code + "' has already been used");
        }

        if (discountCode.getExpiresAt() != null && discountCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Discount code '" + code + "' has expired");
        }

        return discountCode;
    }

    /**
     * Mark a discount code as used by a specific user.
     */
    @CacheEvict(value = "discountCodes", allEntries = true)
    public void markCodeAsUsed(Long discountCodeId, Long userId) {
        DiscountCode discountCode = discountCodeRepository.findById(discountCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("DiscountCode", discountCodeId));

        discountCode.setUsed(true);
        discountCode.setUsedAt(LocalDateTime.now());
        discountCode.setUsedByUserId(userId);
        discountCodeRepository.save(discountCode);
        log.info("Discount code '{}' marked as used by user id '{}'", discountCode.getCode(), userId);
    }

    @CacheEvict(value = "discountCodes", allEntries = true)
    public void deleteDiscountCode(Long id) {
        DiscountCode discountCode = discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DiscountCode", id));
        // Soft delete: deactivate instead of removing
        discountCode.setActive(false);
        discountCodeRepository.save(discountCode);
        log.info("Discount code '{}' deactivated", discountCode.getCode());
    }

    private DiscountCodeResponse toResponse(DiscountCode code) {
        return DiscountCodeResponse.builder()
                .id(code.getId())
                .code(code.getCode())
                .discountAmount(code.getDiscountAmount())
                .active(code.isActive())
                .used(code.isUsed())
                .usedAt(code.getUsedAt())
                .usedByUserId(code.getUsedByUserId())
                .expiresAt(code.getExpiresAt())
                .description(code.getDescription())
                .createdAt(code.getCreatedAt())
                .updatedAt(code.getUpdatedAt())
                .build();
    }
}
