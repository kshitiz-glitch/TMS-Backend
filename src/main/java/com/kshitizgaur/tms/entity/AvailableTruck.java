package com.kshitizgaur.tms.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing available trucks for a Transporter.
 * Each record represents a type of truck and count available.
 */
@Entity
@Table(name = "available_trucks", indexes = {
        @Index(name = "idx_available_trucks_transporter", columnList = "transporter_id"),
        @Index(name = "idx_available_trucks_type", columnList = "truck_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableTruck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;

    @Column(name = "truck_type", nullable = false)
    private String truckType;

    @Column(name = "count", nullable = false)
    private Integer count;

    /**
     * Version field for optimistic locking to prevent concurrent truck allocation
     * conflicts.
     * This is critical for Rule 4: Concurrent Booking Prevention.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Check if this truck type has sufficient capacity.
     */
    public boolean hasSufficientCapacity(int required) {
        return count >= required;
    }

    /**
     * Deduct trucks from available count.
     * 
     * @throws IllegalStateException if insufficient trucks available
     */
    public void deductTrucks(int toDeduct) {
        if (count < toDeduct) {
            throw new IllegalStateException(
                    "Insufficient trucks available. Required: " + toDeduct + ", Available: " + count);
        }
        this.count -= toDeduct;
    }

    /**
     * Restore trucks to available count.
     */
    public void restoreTrucks(int toRestore) {
        this.count += toRestore;
    }
}
