package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.flight.entity.CabinClass;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightPricingRequestDTO {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotNull(message = "Cabin class is required")
    private CabinClass cabinClass;

    @NotNull(message = "Base fare is required")
    @DecimalMin(value = "0.0", message = "Base fare cannot be negative")
    private BigDecimal baseFare;

    @DecimalMin(value = "0.0", message = "Tax cannot be negative")
    private BigDecimal tax;

    @DecimalMin(value = "0.0", message = "Airport fee cannot be negative")
    private BigDecimal airportFee;

    @DecimalMin(value = "0.0", message = "Convenience fee cannot be negative")
    private BigDecimal convenienceFee;

    @DecimalMin(value = "0.0", message = "Baggage fee cannot be negative")
    private BigDecimal baggageFee;

    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    private BigDecimal discount;

    private String currency;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
}