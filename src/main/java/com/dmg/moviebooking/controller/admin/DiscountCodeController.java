package com.dmg.moviebooking.controller.admin;

import com.dmg.moviebooking.dto.request.DiscountCodeRequest;
import com.dmg.moviebooking.dto.response.DiscountCodeResponse;
import com.dmg.moviebooking.service.admin.DiscountCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/discount-codes")
@Tag(name = "Admin - Discount Codes", description = "Admin endpoints for discount code management")
public class DiscountCodeController {

    private final DiscountCodeService discountCodeService;

    public DiscountCodeController(DiscountCodeService discountCodeService) {
        this.discountCodeService = discountCodeService;
    }

    @PostMapping
    @Operation(summary = "Create a new discount code")
    public ResponseEntity<DiscountCodeResponse> createDiscountCode(@Valid @RequestBody DiscountCodeRequest request) {
        DiscountCodeResponse response = discountCodeService.createDiscountCode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all discount codes")
    public ResponseEntity<List<DiscountCodeResponse>> getAllDiscountCodes() {
        return ResponseEntity.ok(discountCodeService.getAllDiscountCodes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a discount code by ID")
    public ResponseEntity<DiscountCodeResponse> getDiscountCodeById(@PathVariable Long id) {
        return ResponseEntity.ok(discountCodeService.getDiscountCodeById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a discount code")
    public ResponseEntity<Void> deleteDiscountCode(@PathVariable Long id) {
        discountCodeService.deleteDiscountCode(id);
        return ResponseEntity.noContent().build();
    }
}
