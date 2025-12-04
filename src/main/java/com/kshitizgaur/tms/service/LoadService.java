package com.kshitizgaur.tms.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kshitizgaur.tms.dto.request.LoadRequestDTO;
import com.kshitizgaur.tms.dto.response.BestBidDTO;
import com.kshitizgaur.tms.dto.response.BidResponseDTO;
import com.kshitizgaur.tms.dto.response.LoadResponseDTO;
import com.kshitizgaur.tms.dto.response.LoadWithBidsDTO;
import com.kshitizgaur.tms.entity.Bid;
import com.kshitizgaur.tms.entity.Load;
import com.kshitizgaur.tms.entity.enums.BidStatus;
import com.kshitizgaur.tms.entity.enums.LoadStatus;
import com.kshitizgaur.tms.exception.InvalidStatusTransitionException;
import com.kshitizgaur.tms.exception.ResourceNotFoundException;
import com.kshitizgaur.tms.repository.BidRepository;
import com.kshitizgaur.tms.repository.BookingRepository;
import com.kshitizgaur.tms.repository.LoadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for Load operations.
 * Implements business rules for load status transitions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LoadService {

    private final LoadRepository loadRepository;
    private final BidRepository bidRepository;
    private final BookingRepository bookingRepository;

    /**
     * Create a new load.
     * Status is set to POSTED by default.
     */
    @Transactional
    public LoadResponseDTO createLoad(LoadRequestDTO request) {
        log.info("Creating load for shipper: {}", request.getShipperId());

        Load load = Load.builder()
                .shipperId(request.getShipperId())
                .loadingCity(request.getLoadingCity())
                .unloadingCity(request.getUnloadingCity())
                .loadingDate(request.getLoadingDate())
                .productType(request.getProductType())
                .weight(request.getWeight())
                .weightUnit(request.getWeightUnit())
                .truckType(request.getTruckType())
                .noOfTrucks(request.getNoOfTrucks())
                .status(LoadStatus.POSTED)
                .build();

        Load savedLoad = loadRepository.save(load);
        log.info("Load created with ID: {}", savedLoad.getLoadId());

        return LoadResponseDTO.fromEntity(savedLoad, savedLoad.getNoOfTrucks(), 0);
    }

    /**
     * Get loads with optional filters and pagination.
     */
    public Page<LoadResponseDTO> getLoads(String shipperId, LoadStatus status, Pageable pageable) {
        Page<Load> loads;

        if (shipperId != null && status != null) {
            loads = loadRepository.findByShipperIdAndStatus(shipperId, status, pageable);
        } else if (shipperId != null) {
            loads = loadRepository.findByShipperId(shipperId, pageable);
        } else if (status != null) {
            loads = loadRepository.findByStatus(status, pageable);
        } else {
            loads = loadRepository.findAll(pageable);
        }

        return loads.map(load -> {
            int allocated = bookingRepository.sumAllocatedTrucksByLoadId(load.getLoadId());
            int remaining = load.getNoOfTrucks() - allocated;
            int activeBids = bidRepository.countPendingBidsByLoadId(load.getLoadId());
            return LoadResponseDTO.fromEntity(load, remaining, activeBids);
        });
    }

    /**
     * Get load by ID with active bids.
     */
    public LoadWithBidsDTO getLoadById(UUID loadId) {
        Load load = loadRepository.findByIdWithBids(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));

        int allocated = bookingRepository.sumAllocatedTrucksByLoadId(loadId);
        int remaining = load.getNoOfTrucks() - allocated;

        List<BidResponseDTO> activeBids = load.getBids().stream()
                .filter(bid -> bid.getStatus() == BidStatus.PENDING)
                .map(BidResponseDTO::fromEntity)
                .collect(Collectors.toList());

        LoadResponseDTO loadDto = LoadResponseDTO.fromEntity(load, remaining, activeBids.size());

        return LoadWithBidsDTO.builder()
                .load(loadDto)
                .activeBids(activeBids)
                .remainingTrucks(remaining)
                .build();
    }

    /**
     * Cancel a load.
     * Rule 2: Cannot cancel load that's already BOOKED.
     */
    @Transactional
    public LoadResponseDTO cancelLoad(UUID loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));

        if (!load.canBeCancelled()) {
            throw new InvalidStatusTransitionException("Load", load.getStatus().name(), "cancel");
        }

        log.info("Cancelling load: {} (current status: {})", loadId, load.getStatus());
        load.setStatus(LoadStatus.CANCELLED);

        // Reject all pending bids
        load.getBids().stream()
                .filter(bid -> bid.getStatus() == BidStatus.PENDING)
                .forEach(bid -> bid.setStatus(BidStatus.REJECTED));

        Load savedLoad = loadRepository.save(load);
        log.info("Load cancelled: {}", loadId);

        return LoadResponseDTO.fromEntity(savedLoad);
    }

    /**
     * Get best bids for a load, sorted by score.
     * Rule 5: score = (1 / proposedRate) * 0.7 + (rating / 5) * 0.3
     */
    public List<BestBidDTO> getBestBids(UUID loadId) {
        // Verify load exists
        if (!loadRepository.existsById(loadId)) {
            throw new ResourceNotFoundException("Load", "loadId", loadId);
        }

        List<Bid> pendingBids = bidRepository.findPendingBidsByLoadId(loadId);

        return pendingBids.stream()
                .map(BestBidDTO::fromEntity)
                .sorted(Comparator.comparingDouble(BestBidDTO::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Transition load status to OPEN_FOR_BIDS.
     * Called when first bid is received.
     */
    @Transactional
    public void transitionToOpenForBids(UUID loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));

        if (load.getStatus() == LoadStatus.POSTED) {
            log.info("Transitioning load {} to OPEN_FOR_BIDS", loadId);
            load.setStatus(LoadStatus.OPEN_FOR_BIDS);
            loadRepository.save(load);
        }
    }

    /**
     * Check and update load status to BOOKED if fully allocated.
     * Rule 3: Load becomes BOOKED only when remainingTrucks == 0.
     */
    @Transactional
    public void checkAndUpdateLoadStatus(UUID loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));

        int allocated = bookingRepository.sumAllocatedTrucksByLoadId(loadId);
        int remaining = load.getNoOfTrucks() - allocated;

        if (remaining == 0 && load.getStatus() == LoadStatus.OPEN_FOR_BIDS) {
            log.info("Load {} fully allocated, transitioning to BOOKED", loadId);
            load.setStatus(LoadStatus.BOOKED);

            // Reject remaining pending bids
            load.getBids().stream()
                    .filter(bid -> bid.getStatus() == BidStatus.PENDING)
                    .forEach(bid -> bid.setStatus(BidStatus.REJECTED));

            loadRepository.save(load);
        }
    }

    /**
     * Revert load status from BOOKED to OPEN_FOR_BIDS if not fully allocated.
     * Called when a booking is cancelled.
     */
    @Transactional
    public void revertLoadStatusIfNeeded(UUID loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));

        int allocated = bookingRepository.sumAllocatedTrucksByLoadId(loadId);
        int remaining = load.getNoOfTrucks() - allocated;

        if (remaining > 0 && load.getStatus() == LoadStatus.BOOKED) {
            log.info("Load {} has remaining trucks, reverting to OPEN_FOR_BIDS", loadId);
            load.setStatus(LoadStatus.OPEN_FOR_BIDS);
            loadRepository.save(load);
        }
    }

    /**
     * Get remaining trucks for a load.
     */
    public int getRemainingTrucks(UUID loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));

        int allocated = bookingRepository.sumAllocatedTrucksByLoadId(loadId);
        return load.getNoOfTrucks() - allocated;
    }

    /**
     * Find load by ID.
     */
    public Load findById(UUID loadId) {
        return loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));
    }
}
