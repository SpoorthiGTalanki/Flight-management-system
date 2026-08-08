package com.flight.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flight.backend.entity.FlightPricing;

@Repository
public interface FlightPricingRepository extends JpaRepository<FlightPricing, Long> {
}
