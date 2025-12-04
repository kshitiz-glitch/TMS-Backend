package com.kshitizgaur.tms.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for registering a new Transporter.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransporterRequestDTO {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Double rating;

    @NotEmpty(message = "At least one truck type is required")
    @Valid
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

        @NotBlank(message = "Truck type is required")
        private String truckType;

        @NotNull(message = "Count is required")
        @Min(value = 0, message = "Count must be non-negative")
        private Integer count;
    }
}
