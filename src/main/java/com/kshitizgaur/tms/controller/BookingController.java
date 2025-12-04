package com.kshitizgaur.tms.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kshitizgaur.tms.dto.request.BookingRequestDTO;
import com.kshitizgaur.tms.dto.response.BookingResponseDTO;
import com.kshitizgaur.tms.service.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Booking operations.
 * Provides 3 endpoints for booking management.
 */
@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Booking management APIs")
public class BookingController {

    private final BookingService bookingService;

    /**
     * 1. POST /booking - Accept bid & create booking (handles concurrency)
     */
    @PostMapping
    @Operation(summary = "Create booking", description = "Accept a bid and create a booking (deducts trucks, handles concurrency)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient capacity"),
            @ApiResponse(responseCode = "404", description = "Bid not found"),
            @ApiResponse(responseCode = "409", description = "Concurrent booking conflict")
    })
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO request) {
        BookingResponseDTO response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. GET /booking/{bookingId} - Get booking details
     */
    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking", description = "Get booking details by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponseDTO> getBookingById(
            @Parameter(description = "Booking ID") @PathVariable UUID bookingId) {
        BookingResponseDTO response = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. PATCH /booking/{bookingId}/cancel - Cancel booking (restores trucks)
     */
    @PatchMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel booking", description = "Cancel a booking (restores trucks, updates load status)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel booking with current status"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponseDTO> cancelBooking(
            @Parameter(description = "Booking ID") @PathVariable UUID bookingId) {
        BookingResponseDTO response = bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok(response);
    }
}
