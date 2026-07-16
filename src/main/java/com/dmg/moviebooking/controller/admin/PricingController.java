package com.dmg.moviebooking.controller.admin;

import com.dmg.moviebooking.dto.request.PricingTierRequest;
import com.dmg.moviebooking.dto.response.PricingTierResponse;
import com.dmg.moviebooking.service.admin.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pricing-tiers")
@Tag(name = "Admin - Pricing Tiers", description = "Admin endpoints for pricing tier management")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping
    @Operation(summary = "Create a new pricing tier")
    public ResponseEntity<PricingTierResponse> createPricingTier(@Valid @RequestBody PricingTierRequest request) {
        PricingTierResponse response = pricingService.createPricingTier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all pricing tiers")
    public ResponseEntity<List<PricingTierResponse>> getAllPricingTiers() {
        return ResponseEntity.ok(pricingService.getAllPricingTiers());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a pricing tier")
    public ResponseEntity<PricingTierResponse> updatePricingTier(@PathVariable Long id,
                                                                  @Valid @RequestBody PricingTierRequest request) {
        return ResponseEntity.ok(pricingService.updatePricingTier(id, request));
    }
}
