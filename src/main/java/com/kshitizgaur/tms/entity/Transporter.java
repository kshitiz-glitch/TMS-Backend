package com.kshitizgaur.tms.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * Entity representing a Transporter in the Transport Management System.
 * A Transporter is a company that can bid on loads and provide trucks for
 * transportation.
 */
@Entity
@Table(name = "transporters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transporter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transporter_id", updatable = false, nullable = false)
    private UUID transporterId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Min(1)
    @Max(5)
    @Column(name = "rating", nullable = false)
    @Builder.Default
    private Double rating = 3.0;

    /**
     * Available trucks owned by this transporter, grouped by truck type.
     */
    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AvailableTruck> availableTrucks = new ArrayList<>();

    /**
     * Bids submitted by this transporter.
     */
    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Bid> bids = new ArrayList<>();

    /**
     * Bookings fulfilled by this transporter.
     */
    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    /**
     * Helper method to add an available truck.
     */
    public void addAvailableTruck(AvailableTruck truck) {
        availableTrucks.add(truck);
        truck.setTransporter(this);
    }

    /**
     * Helper method to remove an available truck.
     */
    public void removeAvailableTruck(AvailableTruck truck) {
        availableTrucks.remove(truck);
        truck.setTransporter(null);
    }

    /**
     * Get available truck count for a specific truck type.
     */
    public int getAvailableTruckCount(String truckType) {
        return availableTrucks.stream()
                .filter(t -> t.getTruckType().equalsIgnoreCase(truckType))
                .mapToInt(AvailableTruck::getCount)
                .sum();
    }
}
