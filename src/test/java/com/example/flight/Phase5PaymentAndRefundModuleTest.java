package com.example.flight;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.example.flight.dto.BookingCancellationResponseDTO;
import com.example.flight.dto.BookingRequestDTO;
import com.example.flight.dto.BookingResponseDTO;
import com.example.flight.dto.PaymentRequestDTO;
import com.example.flight.dto.PaymentResponseDTO;
import com.example.flight.dto.RefundResponseDTO;
import com.example.flight.dto.SeatLockResponseDTO;
import com.example.flight.entity.Aircraft;
import com.example.flight.entity.Airline;
import com.example.flight.entity.Airport;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.CabinClass;
import com.example.flight.entity.Flight;
import com.example.flight.entity.FlightPricing;
import com.example.flight.entity.FlightStatus;
import com.example.flight.entity.Passenger;
import com.example.flight.entity.PaymentMethod;
import com.example.flight.entity.PaymentStatus;
import com.example.flight.entity.RefundStatus;
import com.example.flight.entity.SeatLock;
import com.example.flight.entity.SeatLockStatus;
import com.example.flight.entity.User;
import com.example.flight.exception.BookingAlreadyCancelledException;
import com.example.flight.exception.PaymentAlreadySuccessfulException;
import com.example.flight.exception.RefundNotAllowedException;
import com.example.flight.exception.ResourceNotFoundException;
import com.example.flight.repository.AircraftRepository;
import com.example.flight.repository.AirlineRepository;
import com.example.flight.repository.AirportRepository;
import com.example.flight.repository.BookingCancellationRepository;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.CouponRepository;
import com.example.flight.repository.FlightPricingRepository;
import com.example.flight.repository.FlightRepository;
import com.example.flight.repository.HolidayRepository;
import com.example.flight.repository.PassengerRepository;
import com.example.flight.repository.PaymentRepository;
import com.example.flight.repository.PricingRuleRepository;
import com.example.flight.repository.RefundRepository;
import com.example.flight.repository.SeatLockRepository;
import com.example.flight.repository.UserRepository;
import com.example.flight.service.BookingCancellationService;
import com.example.flight.service.BookingService;
import com.example.flight.service.PaymentService;
import com.example.flight.service.SeatLockService;

@SpringBootTest
@ActiveProfiles("test")
public class Phase5PaymentAndRefundModuleTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingCancellationService bookingCancellationService;

    @Autowired
    private BookingCancellationRepository bookingCancellationRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private FlightPricingRepository flightPricingRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private AirlineRepository airlineRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private SeatLockRepository seatLockRepository;

    @Autowired
    private SeatLockService seatLockService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private org.springframework.cache.CacheManager cacheManager;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private User testUser1;
    private User testUser2;
    private Flight testFlight;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user1@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        java.util.concurrent.ConcurrentHashMap<String, String> redisMemory = new java.util.concurrent.ConcurrentHashMap<>();
        org.springframework.cache.Cache mockCache = org.mockito.Mockito.mock(org.springframework.cache.Cache.class);
        lenient().when(cacheManager.getCache(anyString())).thenReturn(mockCache);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> {
                    String key = inv.getArgument(0);
                    String val = inv.getArgument(1);
                    return redisMemory.putIfAbsent(key, val) == null;
                });
        lenient().when(valueOperations.get(anyString()))
                .thenAnswer(inv -> redisMemory.get(inv.getArgument(0)));
        lenient().when(redisTemplate.hasKey(anyString()))
                .thenAnswer(inv -> redisMemory.containsKey(inv.getArgument(0)));
        lenient().when(redisTemplate.delete(anyString()))
                .thenAnswer(inv -> redisMemory.remove(inv.getArgument(0)) != null);

        refundRepository.deleteAll();
        bookingCancellationRepository.deleteAll();
        paymentRepository.deleteAll();
        seatLockRepository.deleteAll();
        passengerRepository.deleteAll();
        bookingRepository.deleteAll();
        flightPricingRepository.deleteAll();
        flightRepository.deleteAll();
        aircraftRepository.deleteAll();

        userRepository.findByEmail("user1@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("user2@example.com").ifPresent(userRepository::delete);

        testUser1 = userRepository.save(User.builder()
                .email("user1@example.com")
                .passwordHash("pass")
                .firstName("User")
                .lastName("One")
                .role("USER")
                .emailVerified(true)
                .build());

        testUser2 = userRepository.save(User.builder()
                .email("user2@example.com")
                .passwordHash("pass")
                .firstName("User")
                .lastName("Two")
                .role("USER")
                .emailVerified(true)
                .build());

        Aircraft aircraft = aircraftRepository.save(Aircraft.builder()
                .aircraftCode("A320-PMT")
                .model("Airbus A320")
                .manufacturer("Airbus")
                .totalSeatCapacity(180)
                .active(true)
                .build());

        Airline airline = airlineRepository.findById("6E").orElseGet(() ->
                airlineRepository.save(new Airline("6E", "IndiGo"))
        );

        Airport from = airportRepository.findById("DEL").orElseGet(() ->
                airportRepository.save(new Airport("DEL", "Indira Gandhi Int", "Delhi", "India"))
        );

        Airport to = airportRepository.findById("BOM").orElseGet(() ->
                airportRepository.save(new Airport("BOM", "Chhatrapati Shivaji", "Mumbai", "India"))
        );

        testFlight = new Flight();
        testFlight.setFlightNumber("6E-501");
        testFlight.setAirline(airline);
        testFlight.setFromAirport(from);
        testFlight.setToAirport(to);
        testFlight.setAircraft(aircraft);
        testFlight.setDepartureTs(LocalDateTime.now().plusDays(3)); // >24 hours
        testFlight.setArrivalTs(LocalDateTime.now().plusDays(3).plusHours(2));
        testFlight.setStops((short) 0);
        testFlight.setBasePrice(new BigDecimal("4000.00"));
        testFlight.setAvailableSeats((short) 180);
        testFlight.setDurationMins(120);
        testFlight.setStatus(FlightStatus.SCHEDULED);
        testFlight = flightRepository.save(testFlight);

        FlightPricing pricing = FlightPricing.builder()
                .flight(testFlight)
                .seatClass(CabinClass.ECONOMY)
                .baseFare(new BigDecimal("4000.00"))
                .tax(new BigDecimal("400.00"))
                .airportFee(new BigDecimal("100.00"))
                .convenienceFee(new BigDecimal("50.00"))
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .build();
        pricing.calculateFinalPrice();
        flightPricingRepository.save(pricing);
    }

    private static int seatCounter = 1;

    private Booking createTestBooking(User user) {
        BookingRequestDTO req = BookingRequestDTO.builder()
                .flightIds(List.of(testFlight.getFlightId()))
                .cabinClass(CabinClass.ECONOMY)
                .build();

        BookingResponseDTO res = bookingService.createBooking(req, user.getEmail());

        Passenger passenger = new Passenger();
        passenger.setBooking(bookingRepository.findById(res.getBookingId()).get());
        passenger.setFirstName(user.getFirstName());
        passenger.setLastName(user.getLastName());
        passenger.setDateOfBirth(java.time.LocalDate.of(1995, 5, 15));
        passenger = passengerRepository.save(passenger);

        // Lock a unique seat for each test booking
        Long segmentId = res.getSegments().get(0).getSegmentId();
        String seatNumber = (seatCounter++) + "A";
        seatLockService.lockSpecificSeat(testFlight.getFlightId(), segmentId, seatNumber, passenger.getPassengerId(), user.getEmail());

        return bookingRepository.findById(res.getBookingId()).get();
    }

    @Test
    @DisplayName("1. Successful UPI Payment")
    void testSuccessfulUpiPayment() {
        Booking booking = createTestBooking(testUser1);

        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .simulatedOutcome("SUCCESS")
                .build();

        PaymentResponseDTO response = paymentService.processPayment(pmtReq, testUser1.getEmail());

        assertNotNull(response.getPaymentId());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(PaymentMethod.UPI, response.getPaymentMethod());
        assertEquals(new BigDecimal("4550.00"), response.getAmount());
    }

    @Test
    @DisplayName("2. Successful Card Payment")
    void testSuccessfulCardPayment() {
        Booking booking = createTestBooking(testUser1);

        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.CARD)
                .simulatedOutcome("SUCCESS")
                .build();

        PaymentResponseDTO response = paymentService.processPayment(pmtReq, testUser1.getEmail());

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(PaymentMethod.CARD, response.getPaymentMethod());
    }

    @Test
    @DisplayName("3. Successful Net Banking Payment")
    void testSuccessfulNetBankingPayment() {
        Booking booking = createTestBooking(testUser1);

        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.NET_BANKING)
                .simulatedOutcome("SUCCESS")
                .build();

        PaymentResponseDTO response = paymentService.processPayment(pmtReq, testUser1.getEmail());

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(PaymentMethod.NET_BANKING, response.getPaymentMethod());
    }

    @Test
    @DisplayName("4. Successful Wallet Payment")
    void testSuccessfulWalletPayment() {
        Booking booking = createTestBooking(testUser1);

        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.WALLET)
                .simulatedOutcome("SUCCESS")
                .build();

        PaymentResponseDTO response = paymentService.processPayment(pmtReq, testUser1.getEmail());

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(PaymentMethod.WALLET, response.getPaymentMethod());
    }

    @Test
    @DisplayName("5. Failed Payment Simulation")
    void testFailedPaymentSimulation() {
        Booking booking = createTestBooking(testUser1);

        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.CARD)
                .simulatedOutcome("FAILED")
                .build();

        PaymentResponseDTO response = paymentService.processPayment(pmtReq, testUser1.getEmail());

        assertEquals(PaymentStatus.FAILED, response.getStatus());

        Booking updatedBooking = bookingRepository.findById(booking.getBookingId()).get();
        assertEquals(BookingStatus.PAYMENT_FAILED, updatedBooking.getStatus());
    }

    @Test
    @DisplayName("6. Payment Retry with New Transaction ID")
    void testPaymentRetry() {
        Booking booking = createTestBooking(testUser1);

        // Initial Failed Attempt
        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .simulatedOutcome("FAILED")
                .build();
        PaymentResponseDTO failedRes = paymentService.processPayment(pmtReq, testUser1.getEmail());

        // Retry Attempt
        PaymentRequestDTO retryReq = PaymentRequestDTO.builder()
                .paymentMethod(PaymentMethod.CARD)
                .simulatedOutcome("SUCCESS")
                .build();

        PaymentResponseDTO retryRes = paymentService.retryPayment(failedRes.getPaymentId(), retryReq, testUser1.getEmail());

        assertEquals(PaymentStatus.SUCCESS, retryRes.getStatus());
        assertFalse(failedRes.getTransactionRef().equals(retryRes.getTransactionRef()));

        Booking updatedBooking = bookingRepository.findById(booking.getBookingId()).get();
        assertEquals(BookingStatus.CONFIRMED, updatedBooking.getStatus());
    }

    @Test
    @DisplayName("7. Duplicate Successful Payment Prevented")
    void testDuplicateSuccessfulPaymentPrevented() {
        Booking booking = createTestBooking(testUser1);

        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .simulatedOutcome("SUCCESS")
                .build();
        paymentService.processPayment(pmtReq, testUser1.getEmail());

        assertThrows(PaymentAlreadySuccessfulException.class, () ->
                paymentService.processPayment(pmtReq, testUser1.getEmail())
        );
    }

    @Test
    @DisplayName("8. Payment Amount Comes from Server-Side Pricing")
    void testPaymentAmountServerCalculated() {
        Booking booking = createTestBooking(testUser1);

        // Client attempts to pass fake 1.00 amount
        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .amount(new BigDecimal("1.00"))
                .simulatedOutcome("SUCCESS")
                .build();

        PaymentResponseDTO response = paymentService.processPayment(pmtReq, testUser1.getEmail());
        assertEquals(new BigDecimal("4550.00"), response.getAmount());
    }

    @Test
    @DisplayName("9. User Cannot Pay Another User's Booking")
    void testUserCannotPayAnotherUsersBooking() {
        Booking booking = createTestBooking(testUser1);

        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .simulatedOutcome("SUCCESS")
                .build();

        assertThrows(AccessDeniedException.class, () ->
                paymentService.processPayment(pmtReq, testUser2.getEmail())
        );
    }

    @Test
    @DisplayName("10. Payment History Retained Across Retries")
    void testPaymentHistoryRetained() {
        Booking booking = createTestBooking(testUser1);

        PaymentRequestDTO failReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .simulatedOutcome("FAILED")
                .build();
        PaymentResponseDTO failedRes = paymentService.processPayment(failReq, testUser1.getEmail());

        PaymentRequestDTO retryReq = PaymentRequestDTO.builder()
                .paymentMethod(PaymentMethod.CARD)
                .simulatedOutcome("SUCCESS")
                .build();
        paymentService.retryPayment(failedRes.getPaymentId(), retryReq, testUser1.getEmail());

        List<PaymentResponseDTO> history = paymentService.getPaymentsByBooking(booking.getBookingId());
        assertEquals(2, history.size());
    }

    @Test
    @DisplayName("11. Booking Becomes CONFIRMED After Successful Payment")
    void testBookingConfirmedAfterPayment() {
        Booking booking = createTestBooking(testUser1);

        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .simulatedOutcome("SUCCESS")
                .build();

        paymentService.processPayment(pmtReq, testUser1.getEmail());

        Booking dbBooking = bookingRepository.findById(booking.getBookingId()).get();
        assertEquals(BookingStatus.CONFIRMED, dbBooking.getStatus());
        assertEquals(PaymentStatus.SUCCESS, dbBooking.getPaymentStatus());
    }

    @Test
    @DisplayName("12. Seat Lock Becomes CONFIRMED After Successful Payment")
    void testSeatLockConfirmedAfterPayment() {
        Booking booking = createTestBooking(testUser1);

        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .simulatedOutcome("SUCCESS")
                .build();

        paymentService.processPayment(pmtReq, testUser1.getEmail());

        List<SeatLock> locks = seatLockRepository.findByBookingBookingId(booking.getBookingId());
        assertFalse(locks.isEmpty());
        assertEquals(SeatLockStatus.CONFIRMED, locks.get(0).getStatus());
    }

    @Test
    @DisplayName("13. Failed Payment Does Not Confirm Booking Or Seat")
    void testFailedPaymentDoesNotConfirmBookingOrSeat() {
        Booking booking = createTestBooking(testUser1);

        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .simulatedOutcome("FAILED")
                .build();

        paymentService.processPayment(pmtReq, testUser1.getEmail());

        Booking dbBooking = bookingRepository.findById(booking.getBookingId()).get();
        assertEquals(BookingStatus.PAYMENT_FAILED, dbBooking.getStatus());

        List<SeatLock> locks = seatLockRepository.findByBookingBookingId(booking.getBookingId());
        assertEquals(SeatLockStatus.LOCKED, locks.get(0).getStatus());
    }

    @Test
    @DisplayName("14. Transaction ID Generated Uniquely")
    void testUniqueTransactionId() {
        Booking booking1 = createTestBooking(testUser1);
        Booking booking2 = createTestBooking(testUser1);

        PaymentRequestDTO pmtReq1 = PaymentRequestDTO.builder()
                .bookingId(booking1.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .build();

        PaymentRequestDTO pmtReq2 = PaymentRequestDTO.builder()
                .bookingId(booking2.getBookingId())
                .paymentMethod(PaymentMethod.CARD)
                .build();

        PaymentResponseDTO res1 = paymentService.processPayment(pmtReq1, testUser1.getEmail());
        PaymentResponseDTO res2 = paymentService.processPayment(pmtReq2, testUser1.getEmail());

        assertTrue(res1.getTransactionRef().startsWith("TXN-"));
        assertTrue(res2.getTransactionRef().startsWith("TXN-"));
        assertFalse(res1.getTransactionRef().equals(res2.getTransactionRef()));
    }

    @Test
    @DisplayName("15. User Can Cancel Own Eligible Booking")
    void testUserCanCancelOwnBooking() {
        Booking booking = createTestBooking(testUser1);
        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .build();
        paymentService.processPayment(pmtReq, testUser1.getEmail());

        BookingCancellationResponseDTO cancellation = bookingCancellationService.cancelBooking(
                booking.getBookingId(), "Change of plans", testUser1.getEmail()
        );

        assertNotNull(cancellation.getCancellationId());
        assertEquals(booking.getBookingId(), cancellation.getBookingId());

        Booking dbBooking = bookingRepository.findById(booking.getBookingId()).get();
        assertEquals(BookingStatus.CANCELLED, dbBooking.getStatus());
    }

    @Test
    @DisplayName("16. User Cannot Cancel Another User's Booking")
    void testUserCannotCancelAnotherUsersBooking() {
        Booking booking = createTestBooking(testUser1);

        assertThrows(AccessDeniedException.class, () ->
                bookingCancellationService.cancelBooking(booking.getBookingId(), "Hacking", testUser2.getEmail())
        );
    }

    @Test
    @DisplayName("17. Already Cancelled Booking Cannot Be Cancelled Again")
    void testAlreadyCancelledBookingCannotBeCancelledAgain() {
        Booking booking = createTestBooking(testUser1);
        bookingCancellationService.cancelBooking(booking.getBookingId(), "First cancel", testUser1.getEmail());

        assertThrows(BookingAlreadyCancelledException.class, () ->
                bookingCancellationService.cancelBooking(booking.getBookingId(), "Second cancel", testUser1.getEmail())
        );
    }

    @Test
    @DisplayName("18. Full Refund Scenario (>24 hours before departure)")
    void testFullRefundScenario() {
        testFlight.setDepartureTs(LocalDateTime.now().plusHours(36));
        flightRepository.save(testFlight);

        Booking booking = createTestBooking(testUser1);
        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .build();
        paymentService.processPayment(pmtReq, testUser1.getEmail());

        BookingCancellationResponseDTO cancellation = bookingCancellationService.cancelBooking(
                booking.getBookingId(), "Early cancellation", testUser1.getEmail()
        );

        assertEquals(new BigDecimal("0.00"), cancellation.getCancellationCharges());
        assertEquals(new BigDecimal("4550.00"), cancellation.getRefundAmount());
    }

    @Test
    @DisplayName("19. Partial Refund Scenario (4 to 24 hours before departure)")
    void testPartialRefundScenario() {
        testFlight.setDepartureTs(LocalDateTime.now().plusHours(12));
        flightRepository.save(testFlight);

        Booking booking = createTestBooking(testUser1);
        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .build();
        paymentService.processPayment(pmtReq, testUser1.getEmail());

        BookingCancellationResponseDTO cancellation = bookingCancellationService.cancelBooking(
                booking.getBookingId(), "Mid-time cancellation", testUser1.getEmail()
        );

        // 20% of 4550.00 = 910.00 charge, 3640.00 refund
        assertEquals(new BigDecimal("910.00"), cancellation.getCancellationCharges());
        assertEquals(new BigDecimal("3640.00"), cancellation.getRefundAmount());
    }

    @Test
    @DisplayName("20. Cancellation Charge Scenario (<4 hours before departure)")
    void testCancellationChargeScenario() {
        testFlight.setDepartureTs(LocalDateTime.now().plusHours(2));
        flightRepository.save(testFlight);

        Booking booking = createTestBooking(testUser1);
        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .build();
        paymentService.processPayment(pmtReq, testUser1.getEmail());

        BookingCancellationResponseDTO cancellation = bookingCancellationService.cancelBooking(
                booking.getBookingId(), "Last minute cancellation", testUser1.getEmail()
        );

        // 100% charge = 4550.00, 0.00 refund
        assertEquals(new BigDecimal("4550.00"), cancellation.getCancellationCharges());
        assertEquals(new BigDecimal("0.00"), cancellation.getRefundAmount());
    }

    @Test
    @DisplayName("21. Refund Record Created")
    void testRefundRecordCreated() {
        Booking booking = createTestBooking(testUser1);
        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .build();
        paymentService.processPayment(pmtReq, testUser1.getEmail());

        bookingCancellationService.cancelBooking(booking.getBookingId(), "Cancel", testUser1.getEmail());

        RefundResponseDTO refund = bookingCancellationService.getRefundByBooking(booking.getBookingId());
        assertNotNull(refund.getRefundId());
        assertEquals(RefundStatus.SUCCESS, refund.getRefundStatus());
    }

    @Test
    @DisplayName("22. Refund Reference Generated Uniquely")
    void testRefundReferenceGenerated() {
        Booking booking = createTestBooking(testUser1);
        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .build();
        paymentService.processPayment(pmtReq, testUser1.getEmail());

        BookingCancellationResponseDTO cancellation = bookingCancellationService.cancelBooking(
                booking.getBookingId(), "Cancel", testUser1.getEmail()
        );

        assertNotNull(cancellation.getRefundReference());
        assertTrue(cancellation.getRefundReference().startsWith("REF-"));
    }

    @Test
    @DisplayName("23. Booking Status Updated to CANCELLED")
    void testBookingStatusUpdatedToCancelled() {
        Booking booking = createTestBooking(testUser1);
        bookingCancellationService.cancelBooking(booking.getBookingId(), "Cancel", testUser1.getEmail());

        Booking dbBooking = bookingRepository.findById(booking.getBookingId()).get();
        assertEquals(BookingStatus.CANCELLED, dbBooking.getStatus());
    }

    @Test
    @DisplayName("24. Payment Status Updated to REFUNDED")
    void testPaymentStatusUpdatedToRefunded() {
        Booking booking = createTestBooking(testUser1);
        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .build();
        PaymentResponseDTO pmt = paymentService.processPayment(pmtReq, testUser1.getEmail());

        bookingCancellationService.cancelBooking(booking.getBookingId(), "Cancel", testUser1.getEmail());

        PaymentResponseDTO updatedPmt = paymentService.getPaymentById(pmt.getPaymentId());
        assertEquals(PaymentStatus.REFUNDED, updatedPmt.getStatus());
    }

    @Test
    @DisplayName("25. Seat Lock Released Upon Cancellation")
    void testSeatLockReleasedUponCancellation() {
        Booking booking = createTestBooking(testUser1);
        bookingCancellationService.cancelBooking(booking.getBookingId(), "Cancel", testUser1.getEmail());

        List<SeatLock> locks = seatLockRepository.findByBookingBookingId(booking.getBookingId());
        assertFalse(locks.isEmpty());
        assertEquals(SeatLockStatus.RELEASED, locks.get(0).getStatus());
    }

    @Test
    @DisplayName("26. Cannot Refund Unpaid Booking")
    void testCannotRefundUnpaidBooking() {
        Booking booking = createTestBooking(testUser1);
        // Cancel without paying
        bookingCancellationService.cancelBooking(booking.getBookingId(), "Unpaid cancel", testUser1.getEmail());

        assertThrows(ResourceNotFoundException.class, () ->
                bookingCancellationService.getRefundByBooking(booking.getBookingId())
        );
    }

    @Test
    @DisplayName("27. Cannot Refund Twice")
    void testCannotRefundTwice() {
        Booking booking = createTestBooking(testUser1);
        PaymentRequestDTO pmtReq = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .build();
        paymentService.processPayment(pmtReq, testUser1.getEmail());

        bookingCancellationService.cancelBooking(booking.getBookingId(), "First cancel", testUser1.getEmail());

        assertThrows(RefundNotAllowedException.class, () ->
                bookingCancellationService.cancelBooking(booking.getBookingId(), "Second cancel", testUser1.getEmail())
        );
    }
}
