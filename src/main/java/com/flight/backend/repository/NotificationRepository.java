package com.flight.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flight.backend.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
