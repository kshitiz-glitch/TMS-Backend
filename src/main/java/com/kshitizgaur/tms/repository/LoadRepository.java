package com.kshitizgaur.tms.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kshitizgaur.tms.entity.Load;
import com.kshitizgaur.tms.entity.enums.LoadStatus;

/**
 * Repository for Load entity.
 */
@Repository
public interface LoadRepository extends JpaRepository<Load, UUID> {

    /**
     * Find loads by shipper ID with pagination.
     */
    Page<Load> findByShipperId(String shipperId, Pageable pageable);

    /**
     * Find loads by status with pagination.
     */
    Page<Load> findByStatus(LoadStatus status, Pageable pageable);

    /**
     * Find loads by shipper ID and status with pagination.
     */
    Page<Load> findByShipperIdAndStatus(String shipperId, LoadStatus status, Pageable pageable);

    /**
     * Find load with its bids eagerly fetched.
     */
    @Query("SELECT DISTINCT l FROM Load l LEFT JOIN FETCH l.bids b LEFT JOIN FETCH b.transporter WHERE l.loadId = :loadId")
    Optional<Load> findByIdWithBids(@Param("loadId") UUID loadId);

    /**
     * Find load with its bookings eagerly fetched.
     */
    @Query("SELECT DISTINCT l FROM Load l LEFT JOIN FETCH l.bookings WHERE l.loadId = :loadId")
    Optional<Load> findByIdWithBookings(@Param("loadId") UUID loadId);

    /**
     * Count active bids for a load (PENDING status).
     */
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.load.loadId = :loadId AND b.status = 'PENDING'")
    int countActiveBidsByLoadId(@Param("loadId") UUID loadId);
}
