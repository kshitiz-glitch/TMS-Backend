package com.kshitizgaur.tms.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for updating transporter's truck capacity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TruckUpdateDTO {

    @NotEmpty(message = "At least one truck type is required")
    @Valid
    private List<TruckCapacityDTO> trucks;

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
