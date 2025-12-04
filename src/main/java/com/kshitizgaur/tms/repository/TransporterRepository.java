package com.kshitizgaur.tms.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kshitizgaur.tms.entity.Transporter;

/**
 * Repository for Transporter entity.
 */
@Repository
public interface TransporterRepository extends JpaRepository<Transporter, UUID> {

    /**
     * Find transporter with available trucks eagerly fetched.
     */
    @Query("SELECT t FROM Transporter t LEFT JOIN FETCH t.availableTrucks WHERE t.transporterId = :transporterId")
    Optional<Transporter> findByIdWithTrucks(@Param("transporterId") UUID transporterId);

    /**
     * Check if a transporter with the given company name exists.
     */
    boolean existsByCompanyName(String companyName);
}
