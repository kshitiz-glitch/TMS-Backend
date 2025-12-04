package com.kshitizgaur.tms.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kshitizgaur.tms.dto.request.TransporterRequestDTO;
import com.kshitizgaur.tms.dto.request.TruckUpdateDTO;
import com.kshitizgaur.tms.dto.response.TransporterResponseDTO;
import com.kshitizgaur.tms.service.TransporterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Transporter operations.
 * Provides 3 endpoints for transporter management.
 */
@RestController
@RequestMapping("/transporter")
@RequiredArgsConstructor
@Tag(name = "Transporter", description = "Transporter management APIs")
public class TransporterController {

    private final TransporterService transporterService;

    /**
     * 1. POST /transporter - Register transporter with truck capacity
     */
    @PostMapping
    @Operation(summary = "Register transporter", description = "Register a new transporter with truck capacity")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transporter registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<TransporterResponseDTO> registerTransporter(
            @Valid @RequestBody TransporterRequestDTO request) {
        TransporterResponseDTO response = transporterService.registerTransporter(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. GET /transporter/{transporterId} - Get transporter details
     */
    @GetMapping("/{transporterId}")
    @Operation(summary = "Get transporter", description = "Get transporter details by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transporter retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Transporter not found")
    })
    public ResponseEntity<TransporterResponseDTO> getTransporterById(
            @Parameter(description = "Transporter ID") @PathVariable UUID transporterId) {
        TransporterResponseDTO response = transporterService.getTransporterById(transporterId);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. PUT /transporter/{transporterId}/trucks - Update available trucks
     */
    @PutMapping("/{transporterId}/trucks")
    @Operation(summary = "Update trucks", description = "Update available truck capacity")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trucks updated successfully"),
            @ApiResponse(responseCode = "404", description = "Transporter not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<TransporterResponseDTO> updateTrucks(
            @Parameter(description = "Transporter ID") @PathVariable UUID transporterId,
            @Valid @RequestBody TruckUpdateDTO request) {
        TransporterResponseDTO response = transporterService.updateTrucks(transporterId, request);
        return ResponseEntity.ok(response);
    }
}
