package com.example.flight.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.AirlineRequestDTO;
import com.example.flight.dto.AirlineResponseDTO;
import com.example.flight.entity.Airline;
import com.example.flight.exception.ResourceNotFoundException;
import com.example.flight.repository.AirlineRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AirlineService {

    private final AirlineRepository airlineRepository;
    private final ModelMapper modelMapper;

    // ================= ADD AIRLINE =================
    @CacheEvict(value = {"airlines", "airlines_all"}, allEntries = true)
    public AirlineResponseDTO addAirline(AirlineRequestDTO request) {
        if (airlineRepository.existsById(request.getAirlineCode())) {
            throw new RuntimeException("Airline already exists with code: " + request.getAirlineCode());
        }

        Airline airline = modelMapper.map(request, Airline.class);
        Airline savedAirline = airlineRepository.save(airline);

        return modelMapper.map(savedAirline, AirlineResponseDTO.class);
    }

    // ================= GET ALL AIRLINES =================
    @Cacheable(value = "airlines_all")
    @Transactional(readOnly = true)
    public List<AirlineResponseDTO> getAllAirlines() {
        return airlineRepository.findAll()
                .stream()
                .map(airline -> modelMapper.map(airline, AirlineResponseDTO.class))
                .toList();
    }

    // ================= GET AIRLINE =================
    @Cacheable(value = "airlines", key = "#airlineCode")
    @Transactional(readOnly = true)
    public AirlineResponseDTO getAirlineByCode(String airlineCode) {
        Airline airline = airlineRepository.findById(airlineCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with code: " + airlineCode));

        return modelMapper.map(airline, AirlineResponseDTO.class);
    }

    // ================= UPDATE AIRLINE =================
    @CacheEvict(value = {"airlines", "airlines_all"}, allEntries = true)
    public AirlineResponseDTO updateAirline(String airlineCode, AirlineRequestDTO request) {
        Airline airline = airlineRepository.findById(airlineCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with code: " + airlineCode));

        airline.setName(request.getName());
        Airline updatedAirline = airlineRepository.save(airline);

        return modelMapper.map(updatedAirline, AirlineResponseDTO.class);
    }

    // ================= DELETE AIRLINE =================
    @CacheEvict(value = {"airlines", "airlines_all"}, allEntries = true)
    public void deleteAirline(String airlineCode) {
        Airline airline = airlineRepository.findById(airlineCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with code: " + airlineCode));

        airlineRepository.delete(airline);
    }
}