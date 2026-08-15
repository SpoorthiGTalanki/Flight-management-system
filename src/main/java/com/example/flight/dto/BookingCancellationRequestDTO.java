package com.example.flight.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class BookingCancellationRequestDTO {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @Size(
            max = 500,
            message = "Cancellation reason cannot exceed 500 characters"
    )
    private String cancellationReason;

    @DecimalMin(
            value = "0.0",
            message = "Cancellation charges cannot be negative"
    )
    private BigDecimal cancellationCharges;

    @DecimalMin(
            value = "0.0",
            message = "Refund amount cannot be negative"
    )
    private BigDecimal refundAmount;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public BigDecimal getCancellationCharges() { return cancellationCharges; }
    public void setCancellationCharges(BigDecimal cancellationCharges) { this.cancellationCharges = cancellationCharges; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
}