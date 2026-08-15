package com.example.flight.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight.dto.CouponRequestDTO;
import com.example.flight.dto.CouponResponseDTO;
import com.example.flight.service.CouponService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Endpoints for managing promotional coupon codes (ADMIN only)")
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "Create a new coupon code (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public CouponResponseDTO createCoupon(@Valid @RequestBody CouponRequestDTO dto) {
        return couponService.createCoupon(dto);
    }

    @Operation(summary = "Update an existing coupon code (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{couponId}")
    public CouponResponseDTO updateCoupon(@PathVariable Long couponId,
                                          @Valid @RequestBody CouponRequestDTO dto) {
        return couponService.updateCoupon(couponId, dto);
    }

    @Operation(summary = "Delete a coupon code (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{couponId}")
    public void deleteCoupon(@PathVariable Long couponId) {
        couponService.deleteCoupon(couponId);
    }
}
