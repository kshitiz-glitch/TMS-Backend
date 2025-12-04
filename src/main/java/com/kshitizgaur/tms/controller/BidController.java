package com.kshitizgaur.tms.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kshitizgaur.tms.dto.request.BidRequestDTO;
import com.kshitizgaur.tms.dto.response.BidResponseDTO;
import com.kshitizgaur.tms.entity.enums.BidStatus;
import com.kshitizgaur.tms.service.BidService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Bid operations.
 * Provides 4 endpoints for bid management.
 */
@RestController
@RequestMapping("/bid")
@RequiredArgsConstructor
@Tag(name = "Bid", description = "Bid management APIs")
public class BidController {

    private final BidService bidService;

    /**
     * 1. POST /bid - Submit a bid (validates capacity & load status)
     */
    @PostMapping
    @Operation(summary = "Submit bid", description = "Submit a bid for a load (validates capacity & load status)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bid submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request, insufficient capacity, or invalid load status"),
            @ApiResponse(responseCode = "404", description = "Load or transporter not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate bid")
    })
    public ResponseEntity<BidResponseDTO> submitBid(
            @Valid @RequestBody BidRequestDTO request) {
        BidResponseDTO response = bidService.submitBid(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. GET /bid - Filter bids
     */
    @GetMapping
    @Operation(summary = "Filter bids", description = "Get bids with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bids retrieved successfully")
    })
    public ResponseEntity<List<BidResponseDTO>> getBids(
            @Parameter(description = "Filter by load ID") @RequestParam(required = false) UUID loadId,

            @Parameter(description = "Filter by transporter ID") @RequestParam(required = false) UUID transporterId,

            @Parameter(description = "Filter by status") @RequestParam(required = false) BidStatus status) {

        List<BidResponseDTO> response = bidService.getBids(loadId, transporterId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. GET /bid/{bidId} - Get bid details
     */
    @GetMapping("/{bidId}")
    @Operation(summary = "Get bid", description = "Get bid details by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bid retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Bid not found")
    })
    public ResponseEntity<BidResponseDTO> getBidById(
            @Parameter(description = "Bid ID") @PathVariable UUID bidId) {
        BidResponseDTO response = bidService.getBidById(bidId);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. PATCH /bid/{bidId}/reject - Reject a bid
     */
    @PatchMapping("/{bidId}/reject")
    @Operation(summary = "Reject bid", description = "Reject a pending bid")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bid rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot reject bid with current status"),
            @ApiResponse(responseCode = "404", description = "Bid not found")
    })
    public ResponseEntity<BidResponseDTO> rejectBid(
            @Parameter(description = "Bid ID") @PathVariable UUID bidId) {
        BidResponseDTO response = bidService.rejectBid(bidId);
        return ResponseEntity.ok(response);
    }
}
