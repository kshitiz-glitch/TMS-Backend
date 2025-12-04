package com.kshitizgaur.tms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kshitizgaur.tms.dto.request.BidRequestDTO;
import com.kshitizgaur.tms.dto.response.BidResponseDTO;
import com.kshitizgaur.tms.entity.AvailableTruck;
import com.kshitizgaur.tms.entity.Bid;
import com.kshitizgaur.tms.entity.Load;
import com.kshitizgaur.tms.entity.Transporter;
import com.kshitizgaur.tms.entity.enums.BidStatus;
import com.kshitizgaur.tms.entity.enums.LoadStatus;
import com.kshitizgaur.tms.entity.enums.WeightUnit;
import com.kshitizgaur.tms.exception.DuplicateBidException;
import com.kshitizgaur.tms.exception.InsufficientCapacityException;
import com.kshitizgaur.tms.exception.InvalidStatusTransitionException;
import com.kshitizgaur.tms.repository.BidRepository;

/**
 * Unit tests for BidService.
 * Tests capacity validation (Rule 1) and status checks (Rule 2).
 */
@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private LoadService loadService;

    @Mock
    private TransporterService transporterService;

    @InjectMocks
    private BidService bidService;

    private Load testLoad;
    private Transporter testTransporter;
    private UUID loadId;
    private UUID transporterId;

    @BeforeEach
    void setUp() {
        loadId = UUID.randomUUID();
        transporterId = UUID.randomUUID();

        testLoad = Load.builder()
                .loadId(loadId)
                .shipperId("SHIPPER001")
                .loadingCity("Mumbai")
                .unloadingCity("Delhi")
                .productType("Electronics")
                .weight(5000.0)
                .weightUnit(WeightUnit.KG)
                .truckType("TRAILER")
                .noOfTrucks(3)
                .status(LoadStatus.POSTED)
                .bids(new ArrayList<>())
                .build();

        AvailableTruck truck = AvailableTruck.builder()
                .id(UUID.randomUUID())
                .truckType("TRAILER")
                .count(10)
                .build();

        testTransporter = Transporter.builder()
                .transporterId(transporterId)
                .companyName("ABC Transport")
                .rating(4.5)
                .availableTrucks(new ArrayList<>())
                .build();
        testTransporter.addAvailableTruck(truck);
    }

    @Test
    @DisplayName("Should submit bid successfully when all validations pass")
    void submitBid_ShouldSucceed_WhenValidRequest() {
        // Arrange
        BidRequestDTO request = BidRequestDTO.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(50000.0)
                .trucksOffered(2)
                .build();

        Bid savedBid = Bid.builder()
                .bidId(UUID.randomUUID())
                .load(testLoad)
                .transporter(testTransporter)
                .proposedRate(50000.0)
                .trucksOffered(2)
                .status(BidStatus.PENDING)
                .build();

        when(loadService.findById(loadId)).thenReturn(testLoad);
        when(transporterService.findById(transporterId)).thenReturn(testTransporter);
        when(bidRepository.existsByLoadLoadIdAndTransporterTransporterId(loadId, transporterId)).thenReturn(false);
        when(loadService.getRemainingTrucks(loadId)).thenReturn(3);
        when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);
        doNothing().when(loadService).transitionToOpenForBids(loadId);

        // Act
        BidResponseDTO result = bidService.submitBid(request);

        // Assert
        assertNotNull(result);
        assertEquals(BidStatus.PENDING, result.getStatus());
        assertEquals(50000.0, result.getProposedRate());
        verify(loadService).transitionToOpenForBids(loadId);
    }

    @Test
    @DisplayName("Should throw exception when bidding on CANCELLED load - Rule 2")
    void submitBid_ShouldThrowException_WhenLoadCancelled() {
        // Arrange
        testLoad.setStatus(LoadStatus.CANCELLED);

        BidRequestDTO request = BidRequestDTO.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(50000.0)
                .trucksOffered(2)
                .build();

        when(loadService.findById(loadId)).thenReturn(testLoad);

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class, () -> bidService.submitBid(request));
    }

    @Test
    @DisplayName("Should throw exception when bidding on BOOKED load - Rule 2")
    void submitBid_ShouldThrowException_WhenLoadBooked() {
        // Arrange
        testLoad.setStatus(LoadStatus.BOOKED);

        BidRequestDTO request = BidRequestDTO.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(50000.0)
                .trucksOffered(2)
                .build();

        when(loadService.findById(loadId)).thenReturn(testLoad);

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class, () -> bidService.submitBid(request));
    }

    @Test
    @DisplayName("Should throw exception when trucks offered exceeds capacity - Rule 1")
    void submitBid_ShouldThrowException_WhenInsufficientCapacity() {
        // Arrange
        BidRequestDTO request = BidRequestDTO.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(50000.0)
                .trucksOffered(15) // More than available (10)
                .build();

        when(loadService.findById(loadId)).thenReturn(testLoad);
        when(transporterService.findById(transporterId)).thenReturn(testTransporter);
        when(bidRepository.existsByLoadLoadIdAndTransporterTransporterId(loadId, transporterId)).thenReturn(false);
        when(loadService.getRemainingTrucks(loadId)).thenReturn(3);

        // Act & Assert
        assertThrows(InsufficientCapacityException.class, () -> bidService.submitBid(request));
    }

    @Test
    @DisplayName("Should throw exception when transporter already bid on load")
    void submitBid_ShouldThrowException_WhenDuplicateBid() {
        // Arrange
        BidRequestDTO request = BidRequestDTO.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(50000.0)
                .trucksOffered(2)
                .build();

        when(loadService.findById(loadId)).thenReturn(testLoad);
        when(transporterService.findById(transporterId)).thenReturn(testTransporter);
        when(bidRepository.existsByLoadLoadIdAndTransporterTransporterId(loadId, transporterId)).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateBidException.class, () -> bidService.submitBid(request));
    }

    @Test
    @DisplayName("Should reject bid successfully")
    void rejectBid_ShouldSucceed_WhenBidPending() {
        // Arrange
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder()
                .bidId(bidId)
                .load(testLoad)
                .transporter(testTransporter)
                .proposedRate(50000.0)
                .trucksOffered(2)
                .status(BidStatus.PENDING)
                .build();

        when(bidRepository.findByIdWithDetails(bidId)).thenReturn(Optional.of(bid));
        when(bidRepository.save(any(Bid.class))).thenReturn(bid);

        // Act
        BidResponseDTO result = bidService.rejectBid(bidId);

        // Assert
        assertEquals(BidStatus.REJECTED, bid.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when rejecting already accepted bid")
    void rejectBid_ShouldThrowException_WhenBidAccepted() {
        // Arrange
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder()
                .bidId(bidId)
                .load(testLoad)
                .transporter(testTransporter)
                .proposedRate(50000.0)
                .trucksOffered(2)
                .status(BidStatus.ACCEPTED)
                .build();

        when(bidRepository.findByIdWithDetails(bidId)).thenReturn(Optional.of(bid));

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class, () -> bidService.rejectBid(bidId));
    }
}
