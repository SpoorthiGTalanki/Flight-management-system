package com.flight.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flight.backend.entity.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
}
