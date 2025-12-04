package com.kshitizgaur.tms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kshitizgaur.tms.entity.Booking;
import com.kshitizgaur.tms.entity.enums.BookingStatus;

/**
 * Repository for Booking entity.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Find bookings by load ID.
     */
    List<Booking> findByLoadLoadId(UUID loadId);

    /**
     * Find bookings by transporter ID.
     */
    List<Booking> findByTransporterTransporterId(UUID transporterId);

    /**
     * Find booking by bid ID.
     */
    Optional<Booking> findByBidBidId(UUID bidId);

    /**
     * Find confirmed bookings for a load.
     */
    List<Booking> findByLoadLoadIdAndStatus(UUID loadId, BookingStatus status);

    /**
     * Sum of allocated trucks for a load (confirmed bookings only).
     * Critical for Rule 3: Multi-Truck Allocation.
     */
    @Query("SELECT COALESCE(SUM(b.allocatedTrucks), 0) FROM Booking b WHERE b.load.loadId = :loadId AND b.status = 'CONFIRMED'")
    int sumAllocatedTrucksByLoadId(@Param("loadId") UUID loadId);

    /**
     * Find booking by ID with all related entities.
     */
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.load LEFT JOIN FETCH b.bid LEFT JOIN FETCH b.transporter WHERE b.bookingId = :bookingId")
    Optional<Booking> findByIdWithDetails(@Param("bookingId") UUID bookingId);

    /**
     * Check if a booking exists for a bid.
     */
    boolean existsByBidBidId(UUID bidId);

    /**
     * Count confirmed bookings for a load.
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.load.loadId = :loadId AND b.status = 'CONFIRMED'")
    int countConfirmedBookingsByLoadId(@Param("loadId") UUID loadId);
}
