package com.kshitizgaur.tms.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kshitizgaur.tms.entity.AvailableTruck;

import jakarta.persistence.LockModeType;

/**
 * Repository for AvailableTruck entity.
 */
@Repository
public interface AvailableTruckRepository extends JpaRepository<AvailableTruck, UUID> {

    /**
     * Find available truck by transporter ID and truck type.
     */
    Optional<AvailableTruck> findByTransporterTransporterIdAndTruckType(UUID transporterId, String truckType);

    /**
     * Find and lock available truck for update (pessimistic locking for critical
     * sections).
     * This is an alternative to optimistic locking for specific scenarios.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT at FROM AvailableTruck at WHERE at.transporter.transporterId = :transporterId AND at.truckType = :truckType")
    Optional<AvailableTruck> findAndLock(@Param("transporterId") UUID transporterId,
            @Param("truckType") String truckType);

    /**
     * Delete all trucks for a transporter.
     */
    void deleteByTransporterTransporterId(UUID transporterId);
}
