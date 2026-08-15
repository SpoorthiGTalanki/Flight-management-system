package com.example.flight.repository;

import com.example.flight.entity.BookingCancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingCancellationRepository
        extends JpaRepository<BookingCancellation, Long> {

    List<BookingCancellation> findByBookingBookingId(Long bookingId);
}