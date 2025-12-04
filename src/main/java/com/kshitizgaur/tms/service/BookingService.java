package com.kshitizgaur.tms.service;

import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kshitizgaur.tms.dto.request.BookingRequestDTO;
import com.kshitizgaur.tms.dto.response.BookingResponseDTO;
import com.kshitizgaur.tms.entity.AvailableTruck;
import com.kshitizgaur.tms.entity.Bid;
import com.kshitizgaur.tms.entity.Booking;
import com.kshitizgaur.tms.entity.Load;
import com.kshitizgaur.tms.entity.Transporter;
import com.kshitizgaur.tms.entity.enums.BidStatus;
import com.kshitizgaur.tms.entity.enums.BookingStatus;
import com.kshitizgaur.tms.exception.InsufficientCapacityException;
import com.kshitizgaur.tms.exception.InvalidStatusTransitionException;
import com.kshitizgaur.tms.exception.LoadAlreadyBookedException;
import com.kshitizgaur.tms.exception.ResourceNotFoundException;
import com.kshitizgaur.tms.repository.AvailableTruckRepository;
import com.kshitizgaur.tms.repository.BookingRepository;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for Booking operations.
 * Implements concurrent booking prevention (Rule 4) and multi-truck allocation
 * (Rule 3).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AvailableTruckRepository availableTruckRepository;
    private final BidService bidService;
    private final LoadService loadService;
    private final TransporterService transporterService;

    /**
     * Create a booking by accepting a bid.
     * Rule 1: Deduct trucks from transporter capacity.
     * Rule 3: Track multi-truck allocation.
     * Rule 4: Handle concurrent booking with optimistic locking.
     */
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        log.info("Creating booking for bid: {}", request.getBidId());

        try {
            // 1. Find and validate bid
            Bid bid = bidService.findById(request.getBidId());

            if (!bid.canBeAccepted()) {
                throw new InvalidStatusTransitionException("Bid", bid.getStatus().name(), "accept");
            }

            // Check if booking already exists for this bid
            if (bookingRepository.existsByBidBidId(request.getBidId())) {
                throw new LoadAlreadyBookedException("Booking already exists for this bid");
            }

            Load load = bid.getLoad();
            Transporter transporter = bid.getTransporter();

            // 2. Determine trucks to allocate
            int trucksToAllocate = request.getAllocatedTrucks() != null
                    ? request.getAllocatedTrucks()
                    : bid.getTrucksOffered();

            // 3. Validate trucks to allocate doesn't exceed bid offer
            if (trucksToAllocate > bid.getTrucksOffered()) {
                throw new IllegalArgumentException(
                        "Allocated trucks (" + trucksToAllocate + ") cannot exceed trucks offered in bid ("
                                + bid.getTrucksOffered() + ")");
            }

            // 4. Validate remaining trucks needed
            int remainingTrucks = loadService.getRemainingTrucks(load.getLoadId());
            if (trucksToAllocate > remainingTrucks) {
                throw new IllegalArgumentException(
                        "Allocated trucks (" + trucksToAllocate + ") exceeds remaining trucks needed ("
                                + remainingTrucks + ")");
            }

            // 5. Rule 1: Deduct trucks from transporter capacity (with optimistic locking)
            AvailableTruck availableTruck = availableTruckRepository
                    .findByTransporterTransporterIdAndTruckType(transporter.getTransporterId(), load.getTruckType())
                    .orElseThrow(() -> new InsufficientCapacityException(
                            "Transporter does not have trucks of type: " + load.getTruckType()));

            if (!availableTruck.hasSufficientCapacity(trucksToAllocate)) {
                throw new InsufficientCapacityException(
                        load.getTruckType(), trucksToAllocate, availableTruck.getCount());
            }

            // Deduct trucks (this triggers optimistic lock check on save)
            availableTruck.deductTrucks(trucksToAllocate);
            availableTruckRepository.save(availableTruck);

            // 6. Accept the bid
            bid.setStatus(BidStatus.ACCEPTED);

            // 7. Create booking
            Booking booking = Booking.builder()
                    .load(load)
                    .bid(bid)
                    .transporter(transporter)
                    .allocatedTrucks(trucksToAllocate)
                    .finalRate(bid.getProposedRate())
                    .status(BookingStatus.CONFIRMED)
                    .build();

            Booking savedBooking = bookingRepository.save(booking);
            log.info("Booking created with ID: {} (allocated {} trucks)",
                    savedBooking.getBookingId(), trucksToAllocate);

            // 8. Rule 3: Check if load is fully allocated
            loadService.checkAndUpdateLoadStatus(load.getLoadId());

            return BookingResponseDTO.fromEntity(savedBooking);

        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            // Rule 4: Concurrent booking conflict
            log.warn("Concurrent booking conflict for bid: {}", request.getBidId());
            throw new LoadAlreadyBookedException(
                    "Booking conflict detected. Another transaction modified the resource. Please retry.");
        }
    }

    /**
     * Get booking by ID.
     */
    public BookingResponseDTO getBookingById(UUID bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingId", bookingId));

        return BookingResponseDTO.fromEntity(booking);
    }

    /**
     * Cancel a booking.
     * Rule 1: Restore trucks to transporter capacity.
     * Rule 3: Update load status if needed.
     */
    @Transactional
    public BookingResponseDTO cancelBooking(UUID bookingId) {
        log.info("Cancelling booking: {}", bookingId);

        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingId", bookingId));

        if (!booking.canBeCancelled()) {
            throw new InvalidStatusTransitionException("Booking", booking.getStatus().name(), "cancel");
        }

        Load load = booking.getLoad();
        Transporter transporter = booking.getTransporter();

        // 1. Rule 1: Restore trucks to transporter capacity
        AvailableTruck availableTruck = availableTruckRepository
                .findByTransporterTransporterIdAndTruckType(transporter.getTransporterId(), load.getTruckType())
                .orElse(null);

        if (availableTruck != null) {
            availableTruck.restoreTrucks(booking.getAllocatedTrucks());
            availableTruckRepository.save(availableTruck);
            log.info("Restored {} trucks to transporter {}", booking.getAllocatedTrucks(),
                    transporter.getTransporterId());
        }

        // 2. Cancel booking
        booking.setStatus(BookingStatus.CANCELLED);

        // 3. Revert bid status to PENDING (optional - allow rebidding)
        // booking.getBid().setStatus(BidStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking cancelled: {}", bookingId);

        // 4. Rule 3: Revert load status if needed
        loadService.revertLoadStatusIfNeeded(load.getLoadId());

        return BookingResponseDTO.fromEntity(savedBooking);
    }
}
