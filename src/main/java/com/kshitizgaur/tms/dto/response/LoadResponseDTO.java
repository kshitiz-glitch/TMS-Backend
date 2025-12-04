package com.kshitizgaur.tms.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.kshitizgaur.tms.entity.Load;
import com.kshitizgaur.tms.entity.enums.LoadStatus;
import com.kshitizgaur.tms.entity.enums.WeightUnit;

import lombok.*;

/**
 * DTO for Load response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadResponseDTO {

    private UUID loadId;
    private String shipperId;
    private String loadingCity;
    private String unloadingCity;
    private LocalDateTime loadingDate;
    private String productType;
    private Double weight;
    private WeightUnit weightUnit;
    private String truckType;
    private Integer noOfTrucks;
    private LoadStatus status;
    private LocalDateTime datePosted;

    // Computed fields
    private Integer remainingTrucks;
    private Integer activeBidsCount;

    /**
     * Convert Load entity to LoadResponseDTO.
     */
    public static LoadResponseDTO fromEntity(Load load) {
        return LoadResponseDTO.builder()
                .loadId(load.getLoadId())
                .shipperId(load.getShipperId())
                .loadingCity(load.getLoadingCity())
                .unloadingCity(load.getUnloadingCity())
                .loadingDate(load.getLoadingDate())
                .productType(load.getProductType())
                .weight(load.getWeight())
                .weightUnit(load.getWeightUnit())
                .truckType(load.getTruckType())
                .noOfTrucks(load.getNoOfTrucks())
                .status(load.getStatus())
                .datePosted(load.getDatePosted())
                .build();
    }

    /**
     * Convert Load entity to LoadResponseDTO with computed fields.
     */
    public static LoadResponseDTO fromEntity(Load load, int remainingTrucks, int activeBidsCount) {
        LoadResponseDTO dto = fromEntity(load);
        dto.setRemainingTrucks(remainingTrucks);
        dto.setActiveBidsCount(activeBidsCount);
        return dto;
    }
}
