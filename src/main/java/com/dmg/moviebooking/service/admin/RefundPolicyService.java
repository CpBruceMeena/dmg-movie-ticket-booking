package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.RefundPolicyRequest;
import com.dmg.moviebooking.dto.response.RefundPolicyResponse;
import com.dmg.moviebooking.entity.RefundPolicy;
import com.dmg.moviebooking.exception.DuplicateResourceException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.RefundPolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@Transactional
public class RefundPolicyService {

    private final RefundPolicyRepository refundPolicyRepository;

    private static final Logger log = LoggerFactory.getLogger(RefundPolicyService.class);

    public RefundPolicyService(RefundPolicyRepository refundPolicyRepository) {
        this.refundPolicyRepository = refundPolicyRepository;
    }

    @CacheEvict(value = "refundPolicies", allEntries = true)
    public RefundPolicyResponse createRefundPolicy(RefundPolicyRequest request) {
        RefundPolicy policy = RefundPolicy.builder()
                .name(request.getName())
                .hoursBeforeShow(request.getHoursBeforeShow())
                .refundPercentage(request.getRefundPercentage())
                .build();

        policy = refundPolicyRepository.save(policy);
        return toResponse(policy);
    }

    @Cacheable(value = "refundPolicies")
    @Transactional(readOnly = true)
    public List<RefundPolicyResponse> getAllRefundPolicies() {
        return refundPolicyRepository.findAllByOrderByHoursBeforeShowDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @CacheEvict(value = "refundPolicies", allEntries = true)
    public RefundPolicyResponse updateRefundPolicy(Long id, RefundPolicyRequest request) {
        RefundPolicy policy = refundPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RefundPolicy", id));

        policy.setName(request.getName());
        policy.setHoursBeforeShow(request.getHoursBeforeShow());
        policy.setRefundPercentage(request.getRefundPercentage());

        policy = refundPolicyRepository.save(policy);
        return toResponse(policy);
    }

    /**
     * Find the most generous applicable refund policy based on the duration
     * remaining until the show starts.
     * <p>
     * Policies are ordered by hoursBeforeShow DESC (e.g., 48h → 24h → 2h).
     * The first policy where {@code policy.duration <= timeUntilShow}
     * is the most generous applicable one (full nanosecond precision).
     *
     * @param timeUntilShow duration remaining until the show start time
     * @return the applicable RefundPolicy, or null if no policy applies (0% refund)
     */
    @Transactional(readOnly = true)
    public RefundPolicy findApplicablePolicy(Duration timeUntilShow) {
        List<RefundPolicy> policies = refundPolicyRepository.findAllByOrderByHoursBeforeShowDesc();
        for (RefundPolicy policy : policies) {
            Duration policyDuration = Duration.ofHours(policy.getHoursBeforeShow());
            if (policyDuration.compareTo(timeUntilShow) <= 0) {
                log.debug("Applicable refund policy: '{}' ({}%) for {} until show ({}h policy)",
                        policy.getName(), policy.getRefundPercentage(),
                        timeUntilShow, policy.getHoursBeforeShow());
                return policy;
            }
        }
        log.debug("No applicable refund policy found for {} until show", timeUntilShow);
        return null;
    }

    private RefundPolicyResponse toResponse(RefundPolicy policy) {
        return RefundPolicyResponse.builder()
                .id(policy.getId())
                .name(policy.getName())
                .hoursBeforeShow(policy.getHoursBeforeShow())
                .refundPercentage(policy.getRefundPercentage())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
