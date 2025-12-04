package com.kshitizgaur.tms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.kshitizgaur.tms.dto.request.LoadRequestDTO;
import com.kshitizgaur.tms.dto.response.BestBidDTO;
import com.kshitizgaur.tms.dto.response.LoadResponseDTO;
import com.kshitizgaur.tms.dto.response.LoadWithBidsDTO;
import com.kshitizgaur.tms.entity.Bid;
import com.kshitizgaur.tms.entity.Load;
import com.kshitizgaur.tms.entity.Transporter;
import com.kshitizgaur.tms.entity.enums.BidStatus;
import com.kshitizgaur.tms.entity.enums.LoadStatus;
import com.kshitizgaur.tms.entity.enums.WeightUnit;
import com.kshitizgaur.tms.exception.InvalidStatusTransitionException;
import com.kshitizgaur.tms.exception.ResourceNotFoundException;
import com.kshitizgaur.tms.repository.BidRepository;
import com.kshitizgaur.tms.repository.BookingRepository;
import com.kshitizgaur.tms.repository.LoadRepository;

/**
 * Unit tests for LoadService.
 */
@ExtendWith(MockitoExtension.class)
class LoadServiceTest {

    @Mock
    private LoadRepository loadRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private LoadService loadService;

    private Load testLoad;
    private UUID testLoadId;

    @BeforeEach
    void setUp() {
        testLoadId = UUID.randomUUID();
        testLoad = Load.builder()
                .loadId(testLoadId)
                .shipperId("SHIPPER001")
                .loadingCity("Mumbai")
                .unloadingCity("Delhi")
                .loadingDate(LocalDateTime.now().plusDays(1))
                .productType("Electronics")
                .weight(5000.0)
                .weightUnit(WeightUnit.KG)
                .truckType("TRAILER")
                .noOfTrucks(3)
                .status(LoadStatus.POSTED)
                .bids(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should create load with POSTED status")
    void createLoad_ShouldCreateWithPostedStatus() {
        // Arrange
        LoadRequestDTO request = LoadRequestDTO.builder()
                .shipperId("SHIPPER001")
                .loadingCity("Mumbai")
                .unloadingCity("Delhi")
                .loadingDate(LocalDateTime.now().plusDays(1))
                .productType("Electronics")
                .weight(5000.0)
                .weightUnit(WeightUnit.KG)
                .truckType("TRAILER")
                .noOfTrucks(3)
                .build();

        when(loadRepository.save(any(Load.class))).thenReturn(testLoad);

        // Act
        LoadResponseDTO response = loadService.createLoad(request);

        // Assert
        assertNotNull(response);
        assertEquals("SHIPPER001", response.getShipperId());
        assertEquals(LoadStatus.POSTED, response.getStatus());
        verify(loadRepository, times(1)).save(any(Load.class));
    }

    @Test
    @DisplayName("Should return loads with pagination")
    void getLoads_ShouldReturnPaginatedLoads() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Load> loads = List.of(testLoad);
        Page<Load> loadPage = new PageImpl<>(loads, pageable, 1);

        when(loadRepository.findAll(pageable)).thenReturn(loadPage);
        when(bookingRepository.sumAllocatedTrucksByLoadId(any())).thenReturn(0);
        when(bidRepository.countPendingBidsByLoadId(any())).thenReturn(0);

        // Act
        Page<LoadResponseDTO> result = loadService.getLoads(null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Should throw exception when load not found")
    void getLoadById_ShouldThrowExceptionWhenNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(loadRepository.findByIdWithBids(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> loadService.getLoadById(nonExistentId));
    }

    @Test
    @DisplayName("Should return load with active bids")
    void getLoadById_ShouldReturnLoadWithBids() {
        // Arrange
        Transporter transporter = Transporter.builder()
                .transporterId(UUID.randomUUID())
                .companyName("ABC Transport")
                .rating(4.5)
                .build();

        Bid bid = Bid.builder()
                .bidId(UUID.randomUUID())
                .load(testLoad)
                .transporter(transporter)
                .proposedRate(50000.0)
                .trucksOffered(2)
                .status(BidStatus.PENDING)
                .build();

        testLoad.getBids().add(bid);

        when(loadRepository.findByIdWithBids(testLoadId)).thenReturn(Optional.of(testLoad));
        when(bookingRepository.sumAllocatedTrucksByLoadId(testLoadId)).thenReturn(0);

        // Act
        LoadWithBidsDTO result = loadService.getLoadById(testLoadId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getActiveBids().size());
        assertEquals(3, result.getRemainingTrucks());
    }

    @Test
    @DisplayName("Should cancel load with POSTED status")
    void cancelLoad_ShouldCancelPostedLoad() {
        // Arrange
        when(loadRepository.findById(testLoadId)).thenReturn(Optional.of(testLoad));
        when(loadRepository.save(any(Load.class))).thenReturn(testLoad);

        // Act
        LoadResponseDTO result = loadService.cancelLoad(testLoadId);

        // Assert
        assertEquals(LoadStatus.CANCELLED, testLoad.getStatus());
        verify(loadRepository, times(1)).save(testLoad);
    }

    @Test
    @DisplayName("Should throw exception when cancelling BOOKED load")
    void cancelLoad_ShouldThrowExceptionForBookedLoad() {
        // Arrange
        testLoad.setStatus(LoadStatus.BOOKED);
        when(loadRepository.findById(testLoadId)).thenReturn(Optional.of(testLoad));

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class, () -> loadService.cancelLoad(testLoadId));
    }

    @Test
    @DisplayName("Should return best bids sorted by score")
    void getBestBids_ShouldReturnSortedByScore() {
        // Arrange
        Transporter highRatedTransporter = Transporter.builder()
                .transporterId(UUID.randomUUID())
                .companyName("Premium Transport")
                .rating(5.0)
                .build();

        Transporter lowRatedTransporter = Transporter.builder()
                .transporterId(UUID.randomUUID())
                .companyName("Budget Transport")
                .rating(3.0)
                .build();

        Bid highScoreBid = Bid.builder()
                .bidId(UUID.randomUUID())
                .load(testLoad)
                .transporter(highRatedTransporter)
                .proposedRate(40000.0) // Lower rate = higher score
                .trucksOffered(2)
                .status(BidStatus.PENDING)
                .build();

        Bid lowScoreBid = Bid.builder()
                .bidId(UUID.randomUUID())
                .load(testLoad)
                .transporter(lowRatedTransporter)
                .proposedRate(60000.0) // Higher rate = lower score
                .trucksOffered(2)
                .status(BidStatus.PENDING)
                .build();

        when(loadRepository.existsById(testLoadId)).thenReturn(true);
        when(bidRepository.findPendingBidsByLoadId(testLoadId)).thenReturn(List.of(lowScoreBid, highScoreBid));

        // Act
        List<BestBidDTO> result = loadService.getBestBids(testLoadId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        // First bid should have higher score (lower rate + higher rating)
        assertTrue(result.get(0).getScore() > result.get(1).getScore());
    }

    @Test
    @DisplayName("Should transition load to OPEN_FOR_BIDS")
    void transitionToOpenForBids_ShouldUpdateStatus() {
        // Arrange
        when(loadRepository.findById(testLoadId)).thenReturn(Optional.of(testLoad));
        when(loadRepository.save(any(Load.class))).thenReturn(testLoad);

        // Act
        loadService.transitionToOpenForBids(testLoadId);

        // Assert
        assertEquals(LoadStatus.OPEN_FOR_BIDS, testLoad.getStatus());
    }

    @Test
    @DisplayName("Should update load to BOOKED when fully allocated")
    void checkAndUpdateLoadStatus_ShouldMarkAsBookedWhenFullyAllocated() {
        // Arrange
        testLoad.setStatus(LoadStatus.OPEN_FOR_BIDS);
        when(loadRepository.findById(testLoadId)).thenReturn(Optional.of(testLoad));
        when(bookingRepository.sumAllocatedTrucksByLoadId(testLoadId)).thenReturn(3); // All 3 trucks allocated
        when(loadRepository.save(any(Load.class))).thenReturn(testLoad);

        // Act
        loadService.checkAndUpdateLoadStatus(testLoadId);

        // Assert
        assertEquals(LoadStatus.BOOKED, testLoad.getStatus());
    }
}
