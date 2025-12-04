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

import com.kshitizgaur.tms.dto.request.BookingRequestDTO;
import com.kshitizgaur.tms.dto.response.BookingResponseDTO;
import com.kshitizgaur.tms.entity.AvailableTruck;
import com.kshitizgaur.tms.entity.Bid;
import com.kshitizgaur.tms.entity.Booking;
import com.kshitizgaur.tms.entity.Load;
import com.kshitizgaur.tms.entity.Transporter;
import com.kshitizgaur.tms.entity.enums.BidStatus;
import com.kshitizgaur.tms.entity.enums.BookingStatus;
import com.kshitizgaur.tms.entity.enums.LoadStatus;
import com.kshitizgaur.tms.entity.enums.WeightUnit;
import com.kshitizgaur.tms.exception.InsufficientCapacityException;
import com.kshitizgaur.tms.exception.InvalidStatusTransitionException;
import com.kshitizgaur.tms.exception.LoadAlreadyBookedException;
import com.kshitizgaur.tms.repository.AvailableTruckRepository;
import com.kshitizgaur.tms.repository.BookingRepository;

/**
 * Unit tests for BookingService.
 * Tests concurrent booking prevention (Rule 4) and truck allocation (Rule 1,
 * Rule 3).
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailableTruckRepository availableTruckRepository;

    @Mock
    private BidService bidService;

    @Mock
    private LoadService loadService;

    @Mock
    private TransporterService transporterService;

    @InjectMocks
    private BookingService bookingService;

    private Load testLoad;
    private Transporter testTransporter;
    private Bid testBid;
    private AvailableTruck testTruck;
    private UUID loadId;
    private UUID transporterId;
    private UUID bidId;

    @BeforeEach
    void setUp() {
        loadId = UUID.randomUUID();
        transporterId = UUID.randomUUID();
        bidId = UUID.randomUUID();

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
                .status(LoadStatus.OPEN_FOR_BIDS)
                .bids(new ArrayList<>())
                .build();

        testTruck = AvailableTruck.builder()
                .id(UUID.randomUUID())
                .truckType("TRAILER")
                .count(10)
                .version(0L)
                .build();

        testTransporter = Transporter.builder()
                .transporterId(transporterId)
                .companyName("ABC Transport")
                .rating(4.5)
                .availableTrucks(new ArrayList<>())
                .build();
        testTransporter.addAvailableTruck(testTruck);

        testBid = Bid.builder()
                .bidId(bidId)
                .load(testLoad)
                .transporter(testTransporter)
                .proposedRate(50000.0)
                .trucksOffered(2)
                .status(BidStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Should create booking and deduct trucks - Rule 1")
    void createBooking_ShouldDeductTrucks() {
        // Arrange
        BookingRequestDTO request = BookingRequestDTO.builder()
                .bidId(bidId)
                .build();

        Booking savedBooking = Booking.builder()
                .bookingId(UUID.randomUUID())
                .load(testLoad)
                .bid(testBid)
                .transporter(testTransporter)
                .allocatedTrucks(2)
                .finalRate(50000.0)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bidService.findById(bidId)).thenReturn(testBid);
        when(bookingRepository.existsByBidBidId(bidId)).thenReturn(false);
        when(loadService.getRemainingTrucks(loadId)).thenReturn(3);
        when(availableTruckRepository.findByTransporterTransporterIdAndTruckType(transporterId, "TRAILER"))
                .thenReturn(Optional.of(testTruck));
        when(availableTruckRepository.save(any(AvailableTruck.class))).thenReturn(testTruck);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        doNothing().when(loadService).checkAndUpdateLoadStatus(loadId);

        // Act
        BookingResponseDTO result = bookingService.createBooking(request);

        // Assert
        assertNotNull(result);
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(2, result.getAllocatedTrucks());
        assertEquals(8, testTruck.getCount()); // 10 - 2 = 8
        verify(availableTruckRepository).save(testTruck);
    }

    @Test
    @DisplayName("Should throw exception when bid already accepted")
    void createBooking_ShouldThrowException_WhenBidAlreadyAccepted() {
        // Arrange
        testBid.setStatus(BidStatus.ACCEPTED);
        BookingRequestDTO request = BookingRequestDTO.builder()
                .bidId(bidId)
                .build();

        when(bidService.findById(bidId)).thenReturn(testBid);

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class, () -> bookingService.createBooking(request));
    }

    @Test
    @DisplayName("Should throw exception when booking already exists for bid")
    void createBooking_ShouldThrowException_WhenBookingExists() {
        // Arrange
        BookingRequestDTO request = BookingRequestDTO.builder()
                .bidId(bidId)
                .build();

        when(bidService.findById(bidId)).thenReturn(testBid);
        when(bookingRepository.existsByBidBidId(bidId)).thenReturn(true);

        // Act & Assert
        assertThrows(LoadAlreadyBookedException.class, () -> bookingService.createBooking(request));
    }

    @Test
    @DisplayName("Should throw exception when insufficient truck capacity")
    void createBooking_ShouldThrowException_WhenInsufficientCapacity() {
        // Arrange
        testTruck.setCount(1); // Only 1 truck available
        testBid.setTrucksOffered(5); // But 5 offered in bid

        BookingRequestDTO request = BookingRequestDTO.builder()
                .bidId(bidId)
                .build();

        when(bidService.findById(bidId)).thenReturn(testBid);
        when(bookingRepository.existsByBidBidId(bidId)).thenReturn(false);
        when(loadService.getRemainingTrucks(loadId)).thenReturn(5);
        when(availableTruckRepository.findByTransporterTransporterIdAndTruckType(transporterId, "TRAILER"))
                .thenReturn(Optional.of(testTruck));

        // Act & Assert
        assertThrows(InsufficientCapacityException.class, () -> bookingService.createBooking(request));
    }

    @Test
    @DisplayName("Should cancel booking and restore trucks - Rule 1")
    void cancelBooking_ShouldRestoreTrucks() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        testTruck.setCount(8); // After booking was created

        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .load(testLoad)
                .bid(testBid)
                .transporter(testTransporter)
                .allocatedTrucks(2)
                .finalRate(50000.0)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findByIdWithDetails(bookingId)).thenReturn(Optional.of(booking));
        when(availableTruckRepository.findByTransporterTransporterIdAndTruckType(transporterId, "TRAILER"))
                .thenReturn(Optional.of(testTruck));
        when(availableTruckRepository.save(any(AvailableTruck.class))).thenReturn(testTruck);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        doNothing().when(loadService).revertLoadStatusIfNeeded(loadId);

        // Act
        BookingResponseDTO result = bookingService.cancelBooking(bookingId);

        // Assert
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(10, testTruck.getCount()); // 8 + 2 = 10 (restored)
        verify(loadService).revertLoadStatusIfNeeded(loadId);
    }

    @Test
    @DisplayName("Should throw exception when cancelling already cancelled booking")
    void cancelBooking_ShouldThrowException_WhenAlreadyCancelled() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .load(testLoad)
                .bid(testBid)
                .transporter(testTransporter)
                .allocatedTrucks(2)
                .finalRate(50000.0)
                .status(BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findByIdWithDetails(bookingId)).thenReturn(Optional.of(booking));

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class, () -> bookingService.cancelBooking(bookingId));
    }
}
