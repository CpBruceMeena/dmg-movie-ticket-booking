package com.dmg.moviebooking.service.admin;

import com.dmg.moviebooking.dto.request.RefundPolicyRequest;
import com.dmg.moviebooking.dto.response.RefundPolicyResponse;
import com.dmg.moviebooking.entity.RefundPolicy;
import com.dmg.moviebooking.exception.DuplicateResourceException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.RefundPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RefundPolicyService {

    private final RefundPolicyRepository refundPolicyRepository;

    public RefundPolicyService(RefundPolicyRepository refundPolicyRepository) {
        this.refundPolicyRepository = refundPolicyRepository;
    }

    public RefundPolicyResponse createRefundPolicy(RefundPolicyRequest request) {
        RefundPolicy policy = RefundPolicy.builder()
                .name(request.getName())
                .hoursBeforeShow(request.getHoursBeforeShow())
                .refundPercentage(request.getRefundPercentage())
                .build();

        policy = refundPolicyRepository.save(policy);
        return toResponse(policy);
    }

    @Transactional(readOnly = true)
    public List<RefundPolicyResponse> getAllRefundPolicies() {
        return refundPolicyRepository.findAllByOrderByHoursBeforeShowDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public RefundPolicyResponse updateRefundPolicy(Long id, RefundPolicyRequest request) {
        RefundPolicy policy = refundPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RefundPolicy", id));

        policy.setName(request.getName());
        policy.setHoursBeforeShow(request.getHoursBeforeShow());
        policy.setRefundPercentage(request.getRefundPercentage());

        policy = refundPolicyRepository.save(policy);
        return toResponse(policy);
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
