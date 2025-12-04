package com.kshitizgaur.tms.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for creating a Booking (accepting a bid).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequestDTO {

    @NotNull(message = "Bid ID is required")
    private UUID bidId;

    /**
     * Optional: Override the trucks to allocate.
     * If not provided, uses trucksOffered from the bid.
     */
    @Min(value = 1, message = "At least 1 truck must be allocated")
    private Integer allocatedTrucks;
}
