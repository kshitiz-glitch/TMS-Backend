package com.kshitizgaur.tms.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for submitting a new Bid.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidRequestDTO {

    @NotNull(message = "Load ID is required")
    private UUID loadId;

    @NotNull(message = "Transporter ID is required")
    private UUID transporterId;

    @NotNull(message = "Proposed rate is required")
    @Positive(message = "Proposed rate must be positive")
    private Double proposedRate;

    @NotNull(message = "Trucks offered is required")
    @Min(value = 1, message = "At least 1 truck must be offered")
    private Integer trucksOffered;
}
