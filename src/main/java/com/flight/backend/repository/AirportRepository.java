package com.flight.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flight.backend.entity.Airport;

@Repository
public interface AirportRepository extends JpaRepository<Airport, String> {
}
