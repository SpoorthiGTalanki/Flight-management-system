package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCancellationResponseDTO {

    private Long cancellationId;
    private Long bookingId;
    private String cancellationReason;
    private BigDecimal cancellationCharges;
    private BigDecimal refundAmount;
    private String refundReference;
    private LocalDateTime processedAt;
}