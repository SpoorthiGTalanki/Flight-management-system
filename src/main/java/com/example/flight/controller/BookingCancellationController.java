package com.example.flight.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight.dto.BookingCancellationRequestDTO;
import com.example.flight.dto.BookingCancellationResponseDTO;
import com.example.flight.dto.RefundResponseDTO;
import com.example.flight.service.BookingAccessService;
import com.example.flight.service.BookingCancellationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cancellations")
@RequiredArgsConstructor
@Tag(name = "Booking Cancellation & Refund", description = "APIs for flight booking cancellations and refund calculations")
public class BookingCancellationController {

    private final BookingCancellationService cancellationService;
    private final BookingAccessService bookingAccessService;

    // Get all cancellations (ADMIN only)
    @Operation(summary = "Get all cancellations (ADMIN)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingCancellationResponseDTO>> getAllCancellations() {
        return ResponseEntity.ok(cancellationService.getAllCancellations());
    }

    // Get cancellation by ID
    @Operation(summary = "Get cancellation details by ID")
    @GetMapping("/{cancellationId}")
    public ResponseEntity<BookingCancellationResponseDTO> getCancellationById(
            @PathVariable Long cancellationId,
            Authentication authentication) {
        BookingCancellationResponseDTO dto = cancellationService.getCancellationById(cancellationId);
        bookingAccessService.verifyBookingAccess(dto.getBookingId(), authentication);
        return ResponseEntity.ok(dto);
    }

    // Get cancellations by booking ID
    @Operation(summary = "Get cancellations by booking ID")
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<BookingCancellationResponseDTO>> getCancellationsByBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        bookingAccessService.verifyBookingAccess(bookingId, authentication);
        return ResponseEntity.ok(cancellationService.getCancellationsByBooking(bookingId));
    }

    // Get refund details by booking ID
    @Operation(summary = "Get refund details by booking ID")
    @GetMapping("/refund/booking/{bookingId}")
    public ResponseEntity<RefundResponseDTO> getRefundByBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        bookingAccessService.verifyBookingAccess(bookingId, authentication);
        return ResponseEntity.ok(cancellationService.getRefundByBooking(bookingId));
    }

    // Create cancellation
    @Operation(summary = "Cancel booking and initiate refund")
    @PostMapping
    public ResponseEntity<BookingCancellationResponseDTO> createCancellation(
            @Valid @RequestBody BookingCancellationRequestDTO dto,
            Authentication authentication) {
        bookingAccessService.verifyBookingAccess(dto.getBookingId(), authentication);
        String email = authentication.getName();
        BookingCancellationResponseDTO response = cancellationService.cancelBooking(
                dto.getBookingId(),
                dto.getCancellationReason(),
                email
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}