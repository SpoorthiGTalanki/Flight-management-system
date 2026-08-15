package com.example.flight.dto;

import java.math.BigDecimal;

import com.example.flight.entity.PaymentMethod;
import com.example.flight.entity.PaymentStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDTO {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private BigDecimal amount;

    private PaymentStatus status;

    private String transactionRef;

    // Simulated test outcome: "SUCCESS" or "FAILED". Defaults to "SUCCESS" if null.
    private String simulatedOutcome;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getSimulatedOutcome() { return simulatedOutcome; }
    public void setSimulatedOutcome(String simulatedOutcome) { this.simulatedOutcome = simulatedOutcome; }
}