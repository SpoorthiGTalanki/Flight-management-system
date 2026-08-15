package com.example.flight.service;
//Manages passengers

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.flight.dto.PassengerRequestDTO;
import com.example.flight.dto.PassengerResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.Passenger;
import com.example.flight.repository.PassengerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PassengerService {

    private final PassengerRepository passengerRepository;

    private final BookingAccessService bookingAccessService;


    // ================= ADD PASSENGER =================

    public PassengerResponseDTO addPassenger(
            Long bookingId,
            PassengerRequestDTO request,
            String email) {

        // Verify that this booking belongs
        // to the logged-in user.

        Booking booking =
                bookingAccessService.getUserBooking(
                        bookingId,
                        email
                );


        // Create Passenger

        Passenger passenger = new Passenger();

        passenger.setBooking(booking);

        passenger.setFirstName(
                request.getFirstName()
        );

        passenger.setLastName(
                request.getLastName()
        );

        passenger.setDateOfBirth(
                request.getDateOfBirth()
        );

        // Seat will be handled later
        // by the seat-locking module.

        passenger.setSeatNumber(
                request.getSeatNumber()
        );


        Passenger savedPassenger =
                passengerRepository.save(passenger);


        return convertToResponse(savedPassenger);
    }


    // ================= GET PASSENGERS =================

    public List<PassengerResponseDTO> getPassengers(
            Long bookingId,
            String email) {

        // Verify ownership

        bookingAccessService.getUserBooking(
                bookingId,
                email
        );


        return passengerRepository
                .findByBookingBookingId(bookingId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    // ================= CONVERSION =================

    private PassengerResponseDTO convertToResponse(
            Passenger passenger) {

        PassengerResponseDTO response =
                new PassengerResponseDTO();

        response.setPassengerId(
                passenger.getPassengerId()
        );

        response.setBookingId(
                passenger.getBooking()
                        .getBookingId()
        );

        response.setFirstName(
                passenger.getFirstName()
        );

        response.setLastName(
                passenger.getLastName()
        );

        response.setDateOfBirth(
                passenger.getDateOfBirth()
        );

        response.setSeatNumber(
                passenger.getSeatNumber()
        );

        return response;
    }
}