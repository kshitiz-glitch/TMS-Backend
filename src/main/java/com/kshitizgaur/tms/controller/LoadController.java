package com.kshitizgaur.tms.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kshitizgaur.tms.dto.request.LoadRequestDTO;
import com.kshitizgaur.tms.dto.response.BestBidDTO;
import com.kshitizgaur.tms.dto.response.LoadResponseDTO;
import com.kshitizgaur.tms.dto.response.LoadWithBidsDTO;
import com.kshitizgaur.tms.entity.enums.LoadStatus;
import com.kshitizgaur.tms.service.LoadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Load operations.
 * Provides 5 endpoints for load management.
 */
@RestController
@RequestMapping("/load")
@RequiredArgsConstructor
@Tag(name = "Load", description = "Load management APIs")
public class LoadController {

    private final LoadService loadService;

    /**
     * 1. POST /load - Create a new load
     */
    @PostMapping
    @Operation(summary = "Create a new load", description = "Creates a new load with status POSTED")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Load created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<LoadResponseDTO> createLoad(
            @Valid @RequestBody LoadRequestDTO request) {
        LoadResponseDTO response = loadService.createLoad(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. GET /load - List loads with optional filters and pagination
     */
    @GetMapping
    @Operation(summary = "List loads", description = "Get loads with optional filters and pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loads retrieved successfully")
    })
    public ResponseEntity<Page<LoadResponseDTO>> getLoads(
            @Parameter(description = "Filter by shipper ID") @RequestParam(required = false) String shipperId,

            @Parameter(description = "Filter by status") @RequestParam(required = false) LoadStatus status,

            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("datePosted").descending());
        Page<LoadResponseDTO> response = loadService.getLoads(shipperId, status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. GET /load/{loadId} - Get load with active bids
     */
    @GetMapping("/{loadId}")
    @Operation(summary = "Get load by ID", description = "Get load details with active bids")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Load retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Load not found")
    })
    public ResponseEntity<LoadWithBidsDTO> getLoadById(
            @Parameter(description = "Load ID") @PathVariable UUID loadId) {
        LoadWithBidsDTO response = loadService.getLoadById(loadId);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. PATCH /load/{loadId}/cancel - Cancel a load
     */
    @PatchMapping("/{loadId}/cancel")
    @Operation(summary = "Cancel a load", description = "Cancel a load (validates status)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Load cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel load with current status"),
            @ApiResponse(responseCode = "404", description = "Load not found")
    })
    public ResponseEntity<LoadResponseDTO> cancelLoad(
            @Parameter(description = "Load ID") @PathVariable UUID loadId) {
        LoadResponseDTO response = loadService.cancelLoad(loadId);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. GET /load/{loadId}/best-bids - Get sorted bid suggestions
     */
    @GetMapping("/{loadId}/best-bids")
    @Operation(summary = "Get best bids", description = "Get sorted bid suggestions by score")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Best bids retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Load not found")
    })
    public ResponseEntity<List<BestBidDTO>> getBestBids(
            @Parameter(description = "Load ID") @PathVariable UUID loadId) {
        List<BestBidDTO> response = loadService.getBestBids(loadId);
        return ResponseEntity.ok(response);
    }
}
