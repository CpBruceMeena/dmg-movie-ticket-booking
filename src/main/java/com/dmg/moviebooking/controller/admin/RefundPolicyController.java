package com.dmg.moviebooking.controller.admin;

import com.dmg.moviebooking.dto.request.RefundPolicyRequest;
import com.dmg.moviebooking.dto.response.RefundPolicyResponse;
import com.dmg.moviebooking.service.admin.RefundPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/refund-policies")
@Tag(name = "Admin - Refund Policies", description = "Admin endpoints for refund policy management")
public class RefundPolicyController {

    private final RefundPolicyService refundPolicyService;

    public RefundPolicyController(RefundPolicyService refundPolicyService) {
        this.refundPolicyService = refundPolicyService;
    }

    @PostMapping
    @Operation(summary = "Create a new refund policy")
    public ResponseEntity<RefundPolicyResponse> createRefundPolicy(@Valid @RequestBody RefundPolicyRequest request) {
        RefundPolicyResponse response = refundPolicyService.createRefundPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all refund policies")
    public ResponseEntity<List<RefundPolicyResponse>> getAllRefundPolicies() {
        return ResponseEntity.ok(refundPolicyService.getAllRefundPolicies());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a refund policy")
    public ResponseEntity<RefundPolicyResponse> updateRefundPolicy(@PathVariable Long id,
                                                                   @Valid @RequestBody RefundPolicyRequest request) {
        return ResponseEntity.ok(refundPolicyService.updateRefundPolicy(id, request));
    }
}
