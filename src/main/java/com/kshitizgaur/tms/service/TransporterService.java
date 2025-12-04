package com.kshitizgaur.tms.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kshitizgaur.tms.dto.request.TransporterRequestDTO;
import com.kshitizgaur.tms.dto.request.TruckUpdateDTO;
import com.kshitizgaur.tms.dto.response.TransporterResponseDTO;
import com.kshitizgaur.tms.entity.AvailableTruck;
import com.kshitizgaur.tms.entity.Transporter;
import com.kshitizgaur.tms.exception.ResourceNotFoundException;
import com.kshitizgaur.tms.repository.AvailableTruckRepository;
import com.kshitizgaur.tms.repository.TransporterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for Transporter operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransporterService {

    private final TransporterRepository transporterRepository;
    private final AvailableTruckRepository availableTruckRepository;

    /**
     * Register a new transporter with truck capacity.
     */
    @Transactional
    public TransporterResponseDTO registerTransporter(TransporterRequestDTO request) {
        log.info("Registering transporter: {}", request.getCompanyName());

        Transporter transporter = Transporter.builder()
                .companyName(request.getCompanyName())
                .rating(request.getRating() != null ? request.getRating() : 3.0)
                .build();

        // Add available trucks
        if (request.getAvailableTrucks() != null) {
            for (TransporterRequestDTO.TruckCapacityDTO truckDto : request.getAvailableTrucks()) {
                AvailableTruck truck = AvailableTruck.builder()
                        .truckType(truckDto.getTruckType())
                        .count(truckDto.getCount())
                        .build();
                transporter.addAvailableTruck(truck);
            }
        }

        Transporter savedTransporter = transporterRepository.save(transporter);
        log.info("Transporter registered with ID: {}", savedTransporter.getTransporterId());

        return TransporterResponseDTO.fromEntity(savedTransporter);
    }

    /**
     * Get transporter by ID.
     */
    public TransporterResponseDTO getTransporterById(UUID transporterId) {
        Transporter transporter = transporterRepository.findByIdWithTrucks(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Transporter", "transporterId", transporterId));

        return TransporterResponseDTO.fromEntity(transporter);
    }

    /**
     * Update transporter's truck capacity.
     */
    @Transactional
    public TransporterResponseDTO updateTrucks(UUID transporterId, TruckUpdateDTO request) {
        Transporter transporter = transporterRepository.findByIdWithTrucks(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Transporter", "transporterId", transporterId));

        log.info("Updating trucks for transporter: {}", transporterId);

        for (TruckUpdateDTO.TruckCapacityDTO truckDto : request.getTrucks()) {
            // Find existing truck or create new
            AvailableTruck existingTruck = transporter.getAvailableTrucks().stream()
                    .filter(t -> t.getTruckType().equalsIgnoreCase(truckDto.getTruckType()))
                    .findFirst()
                    .orElse(null);

            if (existingTruck != null) {
                existingTruck.setCount(truckDto.getCount());
            } else {
                AvailableTruck newTruck = AvailableTruck.builder()
                        .truckType(truckDto.getTruckType())
                        .count(truckDto.getCount())
                        .build();
                transporter.addAvailableTruck(newTruck);
            }
        }

        Transporter savedTransporter = transporterRepository.save(transporter);
        log.info("Trucks updated for transporter: {}", transporterId);

        return TransporterResponseDTO.fromEntity(savedTransporter);
    }

    /**
     * Find transporter by ID.
     */
    public Transporter findById(UUID transporterId) {
        return transporterRepository.findByIdWithTrucks(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Transporter", "transporterId", transporterId));
    }

    /**
     * Get available truck count for a specific truck type.
     */
    public int getAvailableTruckCount(UUID transporterId, String truckType) {
        Transporter transporter = findById(transporterId);
        return transporter.getAvailableTruckCount(truckType);
    }

    /**
     * Find available truck by transporter and type.
     */
    public AvailableTruck findAvailableTruck(UUID transporterId, String truckType) {
        return availableTruckRepository.findByTransporterTransporterIdAndTruckType(transporterId, truckType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AvailableTruck", "truckType", truckType + " for transporter " + transporterId));
    }
}
