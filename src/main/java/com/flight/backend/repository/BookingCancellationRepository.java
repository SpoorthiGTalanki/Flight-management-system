package com.flight.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flight.backend.entity.BookingCancellation;

@Repository
public interface BookingCancellationRepository extends JpaRepository<BookingCancellation, Long> {
}
