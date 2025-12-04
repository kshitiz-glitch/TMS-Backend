package com.kshitizgaur.tms.dto.response;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.kshitizgaur.tms.entity.AvailableTruck;
import com.kshitizgaur.tms.entity.Transporter;

import lombok.*;

/**
 * DTO for Transporter response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransporterResponseDTO {

    private UUID transporterId;
    private String companyName;
    private Double rating;
    private List<TruckCapacityDTO> availableTrucks;

    /**
     * Nested DTO for truck capacity.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TruckCapacityDTO {
        private String truckType;
        private Integer count;

        public static TruckCapacityDTO fromEntity(AvailableTruck truck) {
            return TruckCapacityDTO.builder()
                    .truckType(truck.getTruckType())
                    .count(truck.getCount())
                    .build();
        }
    }

    /**
     * Convert Transporter entity to TransporterResponseDTO.
     */
    public static TransporterResponseDTO fromEntity(Transporter transporter) {
        List<TruckCapacityDTO> trucks = transporter.getAvailableTrucks() != null
                ? transporter.getAvailableTrucks().stream()
                        .map(TruckCapacityDTO::fromEntity)
                        .collect(Collectors.toList())
                : List.of();

        return TransporterResponseDTO.builder()
                .transporterId(transporter.getTransporterId())
                .companyName(transporter.getCompanyName())
                .rating(transporter.getRating())
                .availableTrucks(trucks)
                .build();
    }
}
