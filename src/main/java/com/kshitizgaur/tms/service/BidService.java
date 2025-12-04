package com.kshitizgaur.tms.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kshitizgaur.tms.dto.request.BidRequestDTO;
import com.kshitizgaur.tms.dto.response.BidResponseDTO;
import com.kshitizgaur.tms.entity.Bid;
import com.kshitizgaur.tms.entity.Load;
import com.kshitizgaur.tms.entity.Transporter;
import com.kshitizgaur.tms.entity.enums.BidStatus;
import com.kshitizgaur.tms.entity.enums.LoadStatus;
import com.kshitizgaur.tms.exception.DuplicateBidException;
import com.kshitizgaur.tms.exception.InsufficientCapacityException;
import com.kshitizgaur.tms.exception.InvalidStatusTransitionException;
import com.kshitizgaur.tms.exception.ResourceNotFoundException;
import com.kshitizgaur.tms.repository.BidRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for Bid operations.
 * Implements capacity validation (Rule 1) and status checks (Rule 2).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;
    private final LoadService loadService;
    private final TransporterService transporterService;

    /**
     * Submit a new bid.
     * Rule 1: Validate transporter has sufficient truck capacity.
     * Rule 2: Validate load status allows bidding.
     */
    @Transactional
    public BidResponseDTO submitBid(BidRequestDTO request) {
        log.info("Submitting bid - Load: {}, Transporter: {}, Trucks: {}",
                request.getLoadId(), request.getTransporterId(), request.getTrucksOffered());

        // 1. Validate load exists and can accept bids
        Load load = loadService.findById(request.getLoadId());
        validateLoadCanAcceptBids(load);

        // 2. Validate transporter exists
        Transporter transporter = transporterService.findById(request.getTransporterId());

        // 3. Check for duplicate bid
        if (bidRepository.existsByLoadLoadIdAndTransporterTransporterId(
                request.getLoadId(), request.getTransporterId())) {
            throw new DuplicateBidException("Transporter has already submitted a bid for this load");
        }

        // 4. Validate trucks offered doesn't exceed remaining trucks needed
        int remainingTrucks = loadService.getRemainingTrucks(request.getLoadId());
        if (request.getTrucksOffered() > remainingTrucks) {
            throw new IllegalArgumentException(
                    "Trucks offered (" + request.getTrucksOffered() + ") exceeds remaining trucks needed ("
                            + remainingTrucks + ")");
        }

        // 5. Rule 1: Validate transporter has sufficient capacity
        int availableCount = transporter.getAvailableTruckCount(load.getTruckType());
        if (request.getTrucksOffered() > availableCount) {
            throw new InsufficientCapacityException(
                    load.getTruckType(), request.getTrucksOffered(), availableCount);
        }

        // 6. Create bid
        Bid bid = Bid.builder()
                .load(load)
                .transporter(transporter)
                .proposedRate(request.getProposedRate())
                .trucksOffered(request.getTrucksOffered())
                .status(BidStatus.PENDING)
                .build();

        Bid savedBid = bidRepository.save(bid);
        log.info("Bid created with ID: {}", savedBid.getBidId());

        // 7. Transition load to OPEN_FOR_BIDS if this is the first bid
        loadService.transitionToOpenForBids(request.getLoadId());

        return BidResponseDTO.fromEntity(savedBid);
    }

    /**
     * Get bids with optional filters.
     */
    public List<BidResponseDTO> getBids(UUID loadId, UUID transporterId, BidStatus status) {
        List<Bid> bids;

        if (loadId != null && status != null) {
            bids = bidRepository.findByLoadLoadIdAndStatus(loadId, status);
        } else if (transporterId != null && status != null) {
            bids = bidRepository.findByTransporterTransporterIdAndStatus(transporterId, status);
        } else if (loadId != null) {
            bids = bidRepository.findByLoadLoadId(loadId);
        } else if (transporterId != null) {
            bids = bidRepository.findByTransporterTransporterId(transporterId);
        } else {
            bids = bidRepository.findAll();
        }

        return bids.stream()
                .map(BidResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get bid by ID.
     */
    public BidResponseDTO getBidById(UUID bidId) {
        Bid bid = bidRepository.findByIdWithDetails(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", bidId));

        return BidResponseDTO.fromEntity(bid);
    }

    /**
     * Reject a bid.
     */
    @Transactional
    public BidResponseDTO rejectBid(UUID bidId) {
        Bid bid = bidRepository.findByIdWithDetails(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", bidId));

        if (!bid.canBeRejected()) {
            throw new InvalidStatusTransitionException("Bid", bid.getStatus().name(), "reject");
        }

        log.info("Rejecting bid: {}", bidId);
        bid.setStatus(BidStatus.REJECTED);

        Bid savedBid = bidRepository.save(bid);
        log.info("Bid rejected: {}", bidId);

        return BidResponseDTO.fromEntity(savedBid);
    }

    /**
     * Find bid by ID.
     */
    public Bid findById(UUID bidId) {
        return bidRepository.findByIdWithDetails(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", bidId));
    }

    /**
     * Accept a bid (mark as ACCEPTED).
     */
    @Transactional
    public void acceptBid(UUID bidId) {
        Bid bid = findById(bidId);

        if (!bid.canBeAccepted()) {
            throw new InvalidStatusTransitionException("Bid", bid.getStatus().name(), "accept");
        }

        bid.setStatus(BidStatus.ACCEPTED);
        bidRepository.save(bid);
        log.info("Bid accepted: {}", bidId);
    }

    /**
     * Validate that load can accept bids.
     * Rule 2: Cannot bid on CANCELLED or BOOKED loads.
     */
    private void validateLoadCanAcceptBids(Load load) {
        if (!load.canAcceptBids()) {
            throw new InvalidStatusTransitionException(
                    "Cannot submit bid for load with status: " + load.getStatus());
        }

        if (load.getStatus() == LoadStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Cannot submit bid for CANCELLED load");
        }

        if (load.getStatus() == LoadStatus.BOOKED) {
            throw new InvalidStatusTransitionException("Cannot submit bid for BOOKED load");
        }
    }
}
