package com.example.flight.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.BookingCancellationRequestDTO;
import com.example.flight.dto.BookingCancellationResponseDTO;
import com.example.flight.dto.RefundResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingCancellation;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.Flight;
import com.example.flight.entity.Payment;
import com.example.flight.entity.PaymentStatus;
import com.example.flight.entity.Refund;
import com.example.flight.entity.RefundStatus;
import com.example.flight.entity.SeatLock;
import com.example.flight.entity.SeatLockStatus;
import com.example.flight.exception.BookingAlreadyCancelledException;
import com.example.flight.exception.CancellationNotAllowedException;
import com.example.flight.exception.RefundNotAllowedException;
import com.example.flight.exception.ResourceNotFoundException;
import com.example.flight.repository.BookingCancellationRepository;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.PaymentRepository;
import com.example.flight.repository.RefundRepository;
import com.example.flight.repository.SeatLockRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingCancellationService {

    private final BookingCancellationRepository cancellationRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final SeatLockRepository seatLockRepository;
    private final SeatLockService seatLockService;
    private final BookingAccessService bookingAccessService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    // Get all cancellations
    public List<BookingCancellationResponseDTO> getAllCancellations() {
        return cancellationRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get cancellation by ID
    public BookingCancellationResponseDTO getCancellationById(Long cancellationId) {
        BookingCancellation cancellation = cancellationRepository.findById(cancellationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cancellation not found with ID: " + cancellationId));
        return convertToResponse(cancellation);
    }

    // Get cancellations by booking ID
    public List<BookingCancellationResponseDTO> getCancellationsByBooking(Long bookingId) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new ResourceNotFoundException("Booking not found with ID: " + bookingId);
        }
        return cancellationRepository.findByBookingBookingId(bookingId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Cancel a booking and process simulated refund if paid.
     */
    @Transactional
    public BookingCancellationResponseDTO cancelBooking(Long bookingId, String cancellationReason, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        // Ownership check
        if (booking.getUser() != null && booking.getUser().getEmail() != null
                && !booking.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You are not authorized to cancel this booking");
        }

        if (refundRepository.existsByBookingBookingId(bookingId)) {
            throw new RefundNotAllowedException("Booking ID " + bookingId + " has already been refunded");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException("Booking ID " + bookingId + " is already cancelled");
        }

        BigDecimal totalAmount = booking.getTotalAmount();
        BigDecimal cancellationCharges = BigDecimal.ZERO;
        BigDecimal refundAmount = BigDecimal.ZERO;
        String refundRef = null;

        // Check if booking was paid
        Optional<Payment> successfulPaymentOpt = paymentRepository.findFirstByBookingBookingIdAndStatusOrderByCreatedAtDesc(
                bookingId, PaymentStatus.SUCCESS);

        if (successfulPaymentOpt.isPresent()) {
            Payment payment = successfulPaymentOpt.get();

            // Calculate departure hours
            Flight flight = booking.getFlight();
            LocalDateTime departureTs = flight != null && flight.getDepartureTs() != null
                    ? flight.getDepartureTs() : LocalDateTime.now().plusHours(48);

            long hoursBeforeDeparture = Duration.between(LocalDateTime.now(), departureTs).toHours();

            if (hoursBeforeDeparture >= 24) {
                // Full refund (>24 hours)
                cancellationCharges = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                refundAmount = totalAmount;
            } else if (hoursBeforeDeparture >= 4) {
                // Partial refund (20% fee, 80% refund)
                cancellationCharges = totalAmount.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
                refundAmount = totalAmount.subtract(cancellationCharges).setScale(2, RoundingMode.HALF_UP);
            } else {
                // Cancellation charge / no refund (<4 hours)
                cancellationCharges = totalAmount;
                refundAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            refundRef = "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

            // Save Refund record
            Refund refund = Refund.builder()
                    .payment(payment)
                    .booking(booking)
                    .refundAmount(refundAmount)
                    .cancellationCharge(cancellationCharges)
                    .refundStatus(RefundStatus.SUCCESS)
                    .refundReference(refundRef)
                    .reason(cancellationReason != null ? cancellationReason : "User requested cancellation")
                    .completedAt(LocalDateTime.now())
                    .build();
            refundRepository.save(refund);

            // Update payment status to REFUNDED
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        // Update booking status to CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Release seats
        List<SeatLock> locks = seatLockRepository.findByBookingBookingId(bookingId);
        for (SeatLock lock : locks) {
            lock.setStatus(SeatLockStatus.RELEASED);
            seatLockRepository.save(lock);

            if (lock.getFlight() != null && lock.getSeatNumber() != null) {
                String lockKey = seatLockService.buildLockKey(lock.getFlight().getFlightId(), lock.getSeatNumber());
                redisTemplate.delete(lockKey);
            }
        }

        // Save BookingCancellation record
        BookingCancellation cancellation = BookingCancellation.builder()
                .booking(booking)
                .cancellationReason(cancellationReason != null ? cancellationReason : "User cancelled booking")
                .cancellationCharges(cancellationCharges)
                .refundAmount(refundAmount)
                .processedAt(LocalDateTime.now())
                .build();

        BookingCancellation savedCancellation = cancellationRepository.save(cancellation);
        BookingCancellationResponseDTO responseDTO = convertToResponse(savedCancellation);
        responseDTO.setRefundReference(refundRef);
        return responseDTO;
    }

    public BookingCancellationResponseDTO createCancellation(BookingCancellationRequestDTO dto) {
        return cancelBooking(dto.getBookingId(), dto.getCancellationReason(), "admin@example.com");
    }

    public RefundResponseDTO getRefundByBooking(Long bookingId) {
        Refund refund = refundRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund record not found for booking ID: " + bookingId));

        return RefundResponseDTO.builder()
                .refundId(refund.getRefundId())
                .paymentId(refund.getPayment() != null ? refund.getPayment().getPaymentId() : null)
                .bookingId(refund.getBooking() != null ? refund.getBooking().getBookingId() : null)
                .refundAmount(refund.getRefundAmount())
                .cancellationCharge(refund.getCancellationCharge())
                .refundStatus(refund.getRefundStatus())
                .refundReference(refund.getRefundReference())
                .reason(refund.getReason())
                .createdAt(refund.getCreatedAt())
                .completedAt(refund.getCompletedAt())
                .build();
    }

    public BookingCancellationResponseDTO convertToResponse(BookingCancellation cancellation) {
        BookingCancellationResponseDTO response = new BookingCancellationResponseDTO();
        response.setCancellationId(cancellation.getCancellationId());
        response.setBookingId(cancellation.getBooking() != null ? cancellation.getBooking().getBookingId() : null);
        response.setCancellationReason(cancellation.getCancellationReason());
        response.setCancellationCharges(cancellation.getCancellationCharges());
        response.setRefundAmount(cancellation.getRefundAmount());
        response.setProcessedAt(cancellation.getProcessedAt());

        refundRepository.findByBookingBookingId(cancellation.getBooking().getBookingId())
                .ifPresent(refund -> response.setRefundReference(refund.getRefundReference()));

        return response;
    }
}