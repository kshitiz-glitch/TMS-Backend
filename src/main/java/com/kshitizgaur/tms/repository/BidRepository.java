package com.kshitizgaur.tms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kshitizgaur.tms.entity.Bid;
import com.kshitizgaur.tms.entity.enums.BidStatus;

/**
 * Repository for Bid entity.
 */
@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {

    /**
     * Find bids by load ID.
     */
    List<Bid> findByLoadLoadId(UUID loadId);

    /**
     * Find bids by transporter ID.
     */
    List<Bid> findByTransporterTransporterId(UUID transporterId);

    /**
     * Find bids by load ID and status.
     */
    List<Bid> findByLoadLoadIdAndStatus(UUID loadId, BidStatus status);

    /**
     * Find bids by transporter ID and status.
     */
    List<Bid> findByTransporterTransporterIdAndStatus(UUID transporterId, BidStatus status);

    /**
     * Check if a transporter has already bid on a load.
     */
    boolean existsByLoadLoadIdAndTransporterTransporterId(UUID loadId, UUID transporterId);

    /**
     * Find bid by ID with transporter eagerly fetched.
     */
    @Query("SELECT b FROM Bid b LEFT JOIN FETCH b.transporter LEFT JOIN FETCH b.load WHERE b.bidId = :bidId")
    Optional<Bid> findByIdWithDetails(@Param("bidId") UUID bidId);

    /**
     * Find pending bids for a load with transporter details.
     */
    @Query("SELECT b FROM Bid b JOIN FETCH b.transporter WHERE b.load.loadId = :loadId AND b.status = 'PENDING' ORDER BY b.submittedAt ASC")
    List<Bid> findPendingBidsByLoadId(@Param("loadId") UUID loadId);

    /**
     * Count pending bids for a load.
     */
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.load.loadId = :loadId AND b.status = 'PENDING'")
    int countPendingBidsByLoadId(@Param("loadId") UUID loadId);
}
