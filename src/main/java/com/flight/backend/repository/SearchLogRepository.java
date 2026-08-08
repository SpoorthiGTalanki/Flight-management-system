package com.flight.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flight.backend.entity.SearchLog;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
}
