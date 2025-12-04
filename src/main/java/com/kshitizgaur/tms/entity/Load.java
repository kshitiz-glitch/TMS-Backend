package com.kshitizgaur.tms.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.kshitizgaur.tms.entity.enums.LoadStatus;
import com.kshitizgaur.tms.entity.enums.WeightUnit;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a Load in the Transport Management System.
 * A Load is a shipment request posted by a shipper that transporters can bid
 * on.
 */
@Entity
@Table(name = "loads", indexes = {
        @Index(name = "idx_load_shipper_status", columnList = "shipperId, status"),
        @Index(name = "idx_load_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Load {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "load_id", updatable = false, nullable = false)
    private UUID loadId;

    @Column(name = "shipper_id", nullable = false)
    private String shipperId;

    @Column(name = "loading_city", nullable = false)
    private String loadingCity;

    @Column(name = "unloading_city", nullable = false)
    private String unloadingCity;

    @Column(name = "loading_date", nullable = false)
    private LocalDateTime loadingDate;

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_unit", nullable = false)
    private WeightUnit weightUnit;

    @Column(name = "truck_type", nullable = false)
    private String truckType;

    @Column(name = "no_of_trucks", nullable = false)
    private Integer noOfTrucks;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private LoadStatus status = LoadStatus.POSTED;

    @Column(name = "date_posted", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime datePosted = LocalDateTime.now();

    /**
     * Version field for optimistic locking to prevent concurrent booking conflicts.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Bids received for this load.
     */
    @OneToMany(mappedBy = "load", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Bid> bids = new ArrayList<>();

    /**
     * Bookings made for this load.
     */
    @OneToMany(mappedBy = "load", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    /**
     * Check if the load can accept new bids.
     */
    public boolean canAcceptBids() {
        return status == LoadStatus.POSTED || status == LoadStatus.OPEN_FOR_BIDS;
    }

    /**
     * Check if the load can be cancelled.
     */
    public boolean canBeCancelled() {
        return status != LoadStatus.BOOKED && status != LoadStatus.CANCELLED;
    }
}
