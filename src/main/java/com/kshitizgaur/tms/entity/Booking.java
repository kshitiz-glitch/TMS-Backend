package com.kshitizgaur.tms.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.kshitizgaur.tms.entity.enums.BookingStatus;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a Booking in the Transport Management System.
 * A Booking is created when a bid is accepted, allocating trucks for a load.
 */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_load_id", columnList = "load_id"),
        @Index(name = "idx_booking_transporter_id", columnList = "transporter_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id", updatable = false, nullable = false)
    private UUID bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id", nullable = false)
    private Load load;

    /**
     * One-to-one relationship with Bid.
     * Unique constraint ensures one accepted bid per booking.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false, unique = true)
    private Bid bid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;

    @Column(name = "allocated_trucks", nullable = false)
    private Integer allocatedTrucks;

    @Column(name = "final_rate", nullable = false)
    private Double finalRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(name = "booked_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime bookedAt = LocalDateTime.now();

    /**
     * Check if the booking can be cancelled.
     */
    public boolean canBeCancelled() {
        return status == BookingStatus.CONFIRMED;
    }

    /**
     * Check if the booking is active (confirmed or completed).
     */
    public boolean isActive() {
        return status == BookingStatus.CONFIRMED || status == BookingStatus.COMPLETED;
    }
}
