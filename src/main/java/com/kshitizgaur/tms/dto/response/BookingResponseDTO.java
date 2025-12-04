package com.kshitizgaur.tms.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.kshitizgaur.tms.entity.Booking;
import com.kshitizgaur.tms.entity.enums.BookingStatus;

import lombok.*;

/**
 * DTO for Booking response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {

    private UUID bookingId;
    private UUID loadId;
    private UUID bidId;
    private UUID transporterId;
    private String transporterName;
    private Integer allocatedTrucks;
    private Double finalRate;
    private BookingStatus status;
    private LocalDateTime bookedAt;

    // Additional context
    private String loadingCity;
    private String unloadingCity;
    private String truckType;

    /**
     * Convert Booking entity to BookingResponseDTO.
     */
    public static BookingResponseDTO fromEntity(Booking booking) {
        return BookingResponseDTO.builder()
                .bookingId(booking.getBookingId())
                .loadId(booking.getLoad() != null ? booking.getLoad().getLoadId() : null)
                .bidId(booking.getBid() != null ? booking.getBid().getBidId() : null)
                .transporterId(booking.getTransporter() != null ? booking.getTransporter().getTransporterId() : null)
                .transporterName(booking.getTransporter() != null ? booking.getTransporter().getCompanyName() : null)
                .allocatedTrucks(booking.getAllocatedTrucks())
                .finalRate(booking.getFinalRate())
                .status(booking.getStatus())
                .bookedAt(booking.getBookedAt())
                .loadingCity(booking.getLoad() != null ? booking.getLoad().getLoadingCity() : null)
                .unloadingCity(booking.getLoad() != null ? booking.getLoad().getUnloadingCity() : null)
                .truckType(booking.getLoad() != null ? booking.getLoad().getTruckType() : null)
                .build();
    }
}
