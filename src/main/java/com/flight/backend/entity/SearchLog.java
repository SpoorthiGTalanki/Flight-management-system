package com.flight.backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Search_Logs", schema = "flight_booking_system")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_id")
    private Long searchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "from_airport", length = 3)
    private String fromAirport;

    @Column(name = "to_airport", length = 3)
    private String toAirport;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "passengers")
    private Integer passengers;

    @Column(name = "filters", columnDefinition = "jsonb")
    private String filters;

    @Column(name = "searched_at", insertable = false, updatable = false)
    private LocalDateTime searchedAt;
}
