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

import com.example.flight.dto.PaymentRequestDTO;
import com.example.flight.dto.PaymentResponseDTO;
import com.example.flight.service.BookingAccessService;
import com.example.flight.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "APIs for simulated flight booking payments and retries")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingAccessService bookingAccessService;

    // Get all payments (ADMIN only)
    @Operation(summary = "Get all payments (ADMIN)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @Operation(summary = "Get all payments alias (ADMIN)")
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getAllPaymentsAlias() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    // Get logged-in user's payment history
    @Operation(summary = "Get my payments")
    @GetMapping("/my-payments")
    public ResponseEntity<List<PaymentResponseDTO>> getMyPayments(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(paymentService.getUserPayments(email));
    }

    // Get payment by ID (Owner or ADMIN)
    @Operation(summary = "Get payment by ID")
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(
            @PathVariable Long paymentId,
            Authentication authentication) {
        bookingAccessService.verifyPaymentAccess(paymentId, authentication);
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    // Get payments by booking (Owner or ADMIN)
    @Operation(summary = "Get payments by booking ID")
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        bookingAccessService.verifyBookingAccess(bookingId, authentication);
        return ResponseEntity.ok(paymentService.getPaymentsByBooking(bookingId));
    }

    // Get payment by transaction reference (Owner or ADMIN)
    @Operation(summary = "Get payment by transaction reference")
    @GetMapping("/transaction/{transactionRef}")
    public ResponseEntity<PaymentResponseDTO> getPaymentByTransactionRef(
            @PathVariable String transactionRef,
            Authentication authentication) {
        PaymentResponseDTO payment = paymentService.getPaymentByTransactionRef(transactionRef);
        bookingAccessService.verifyPaymentAccess(payment.getPaymentId(), authentication);
        return ResponseEntity.ok(payment);
    }

    // Process payment (Simulated payment, ownership checked)
    @Operation(summary = "Process simulated payment for booking")
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody PaymentRequestDTO dto,
            Authentication authentication) {
        bookingAccessService.verifyBookingAccess(dto.getBookingId(), authentication);
        String email = authentication.getName();
        PaymentResponseDTO response = paymentService.processPayment(dto, email);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Retry failed payment (Owner or ADMIN)
    @Operation(summary = "Retry a failed payment")
    @PostMapping("/{paymentId}/retry")
    public ResponseEntity<PaymentResponseDTO> retryPayment(
            @PathVariable Long paymentId,
            @RequestBody(required = false) PaymentRequestDTO retryDto,
            Authentication authentication) {
        bookingAccessService.verifyPaymentAccess(paymentId, authentication);
        String email = authentication.getName();
        return ResponseEntity.ok(paymentService.retryPayment(paymentId, retryDto, email));
    }
}