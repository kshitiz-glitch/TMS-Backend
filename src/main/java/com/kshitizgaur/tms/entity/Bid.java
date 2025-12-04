package com.kshitizgaur.tms.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.kshitizgaur.tms.entity.enums.BidStatus;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a Bid in the Transport Management System.
 * A Bid is a proposal from a transporter to fulfill a load.
 */
@Entity
@Table(name = "bids", indexes = {
        @Index(name = "idx_bid_load_id", columnList = "load_id"),
        @Index(name = "idx_bid_transporter_id", columnList = "transporter_id"),
        @Index(name = "idx_bid_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "bid_id", updatable = false, nullable = false)
    private UUID bidId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id", nullable = false)
    private Load load;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;

    @Column(name = "proposed_rate", nullable = false)
    private Double proposedRate;

    @Column(name = "trucks_offered", nullable = false)
    private Integer trucksOffered;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BidStatus status = BidStatus.PENDING;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    /**
     * Associated booking if bid is accepted.
     */
    @OneToOne(mappedBy = "bid", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Booking booking;

    /**
     * Check if the bid can be accepted.
     */
    public boolean canBeAccepted() {
        return status == BidStatus.PENDING;
    }

    /**
     * Check if the bid can be rejected.
     */
    public boolean canBeRejected() {
        return status == BidStatus.PENDING;
    }

    /**
     * Calculate bid score for ranking.
     * Formula: score = (1 / proposedRate) * 0.7 + (rating / 5) * 0.3
     * Higher score = better bid
     */
    public double calculateScore() {
        double transporterRating = transporter != null ? transporter.getRating() : 3.0;
        return (1.0 / proposedRate) * 0.7 + (transporterRating / 5.0) * 0.3;
    }
}
