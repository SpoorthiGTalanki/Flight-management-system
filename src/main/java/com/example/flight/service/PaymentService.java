package com.example.flight.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.PaymentRequestDTO;
import com.example.flight.dto.PaymentResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.Payment;
import com.example.flight.entity.PaymentStatus;
import com.example.flight.entity.SeatLock;
import com.example.flight.entity.SeatLockStatus;
import com.example.flight.exception.BookingAlreadyCancelledException;
import com.example.flight.exception.PaymentAlreadySuccessfulException;
import com.example.flight.exception.PaymentFailedException;
import com.example.flight.exception.PaymentNotAllowedException;
import com.example.flight.exception.PaymentNotFoundException;
import com.example.flight.exception.ResourceNotFoundException;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.PaymentRepository;
import com.example.flight.repository.SeatLockRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final SeatLockRepository seatLockRepository;
    private final BookingAccessService bookingAccessService;

    // Get all payments (ADMIN)
    public List<PaymentResponseDTO> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get payment by ID
    public PaymentResponseDTO getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));
        return convertToResponse(payment);
    }

    // Get payments by booking ID
    public List<PaymentResponseDTO> getPaymentsByBooking(Long bookingId) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new ResourceNotFoundException("Booking not found with ID: " + bookingId);
        }
        return paymentRepository.findByBookingBookingIdOrderByCreatedAtDesc(bookingId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get payments for logged in user
    public List<PaymentResponseDTO> getUserPayments(String email) {
        return paymentRepository.findByBookingUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get payment by transaction reference
    public PaymentResponseDTO getPaymentByTransactionRef(String transactionRef) {
        Payment payment = paymentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with transaction reference: " + transactionRef));
        return convertToResponse(payment);
    }

    /**
     * Process simulated payment for booking.
     */
    @Transactional
    public PaymentResponseDTO processPayment(PaymentRequestDTO dto, String userEmail) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + dto.getBookingId()));

        // Check ownership
        if (booking.getUser() != null && booking.getUser().getEmail() != null
                && !booking.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You are not authorized to pay for this booking");
        }

        // Validate booking status
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException("Cannot pay for a cancelled booking");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED
                || paymentRepository.existsByBookingBookingIdAndStatus(booking.getBookingId(), PaymentStatus.SUCCESS)) {
            throw new PaymentAlreadySuccessfulException("Booking ID " + dto.getBookingId() + " is already paid and confirmed");
        }

        // SERVER-SIDE PRICING ENFORCEMENT: Ignore client amount and take server total
        java.math.BigDecimal payableAmount = booking.getTotalAmount();

        // Generate unique transaction ID
        String txnRef = dto.getTransactionRef();
        if (txnRef == null || txnRef.isBlank() || paymentRepository.existsByTransactionRef(txnRef)) {
            txnRef = generateTransactionRef();
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(dto.getPaymentMethod())
                .amount(payableAmount)
                .currency("INR")
                .transactionRef(txnRef)
                .status(PaymentStatus.PENDING)
                .build();

        // Simulate payment outcome: FAILED if simulatedOutcome == "FAILED", else SUCCESS
        boolean isSuccess = !"FAILED".equalsIgnoreCase(dto.getSimulatedOutcome());

        if (isSuccess) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentStatus(PaymentStatus.SUCCESS);

            // Confirm seat locks
            List<SeatLock> locks = seatLockRepository.findByBookingBookingId(booking.getBookingId());
            for (SeatLock lock : locks) {
                if (lock.getStatus() == SeatLockStatus.LOCKED) {
                    lock.setStatus(SeatLockStatus.CONFIRMED);
                    seatLockRepository.save(lock);
                }
            }
        } else {
            payment.setStatus(PaymentStatus.FAILED);

            booking.setStatus(BookingStatus.PAYMENT_FAILED);
            booking.setPaymentStatus(PaymentStatus.FAILED);
        }

        bookingRepository.save(booking);
        Payment savedPayment = paymentRepository.save(payment);

        if (!isSuccess) {
            // Throw exception or return failed response DTO
            // Returning failed payment DTO allows retry and history tracking
        }

        return convertToResponse(savedPayment);
    }

    /**
     * Retry a FAILED payment.
     */
    @Transactional
    public PaymentResponseDTO retryPayment(Long paymentId, PaymentRequestDTO retryDto, String userEmail) {
        Payment originalPayment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        Booking booking = originalPayment.getBooking();

        // Ownership check
        if (booking.getUser() != null && booking.getUser().getEmail() != null
                && !booking.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You are not authorized to retry payment for this booking");
        }

        if (originalPayment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentAlreadySuccessfulException("Payment was already successful and cannot be retried");
        }

        if (originalPayment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentNotAllowedException("Refunded payment cannot be retried");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException("Cannot retry payment for a cancelled booking");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED
                || paymentRepository.existsByBookingBookingIdAndStatus(booking.getBookingId(), PaymentStatus.SUCCESS)) {
            throw new PaymentAlreadySuccessfulException("Booking is already paid and confirmed");
        }

        // Generate NEW transaction reference to preserve payment attempt history
        String newTxnRef = generateTransactionRef();

        Payment newPaymentAttempt = Payment.builder()
                .booking(booking)
                .paymentMethod(retryDto != null && retryDto.getPaymentMethod() != null ? retryDto.getPaymentMethod() : originalPayment.getPaymentMethod())
                .amount(booking.getTotalAmount())
                .currency("INR")
                .transactionRef(newTxnRef)
                .status(PaymentStatus.PENDING)
                .build();

        boolean isSuccess = retryDto == null || !"FAILED".equalsIgnoreCase(retryDto.getSimulatedOutcome());

        if (isSuccess) {
            newPaymentAttempt.setStatus(PaymentStatus.SUCCESS);
            newPaymentAttempt.setPaidAt(LocalDateTime.now());

            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentStatus(PaymentStatus.SUCCESS);

            List<SeatLock> locks = seatLockRepository.findByBookingBookingId(booking.getBookingId());
            for (SeatLock lock : locks) {
                if (lock.getStatus() == SeatLockStatus.LOCKED) {
                    lock.setStatus(SeatLockStatus.CONFIRMED);
                    seatLockRepository.save(lock);
                }
            }
        } else {
            newPaymentAttempt.setStatus(PaymentStatus.FAILED);
            booking.setStatus(BookingStatus.PAYMENT_FAILED);
            booking.setPaymentStatus(PaymentStatus.FAILED);
        }

        bookingRepository.save(booking);
        Payment savedNewPayment = paymentRepository.save(newPaymentAttempt);

        return convertToResponse(savedNewPayment);
    }

    private String generateTransactionRef() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    public PaymentResponseDTO convertToResponse(Payment payment) {
        return PaymentResponseDTO.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBooking() != null ? payment.getBooking().getBookingId() : null)
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .transactionRef(payment.getTransactionRef())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}