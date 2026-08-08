package com.flight.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flight.backend.entity.Airline;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, String> {
}
