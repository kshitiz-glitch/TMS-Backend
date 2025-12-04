package com.kshitizgaur.tms.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.kshitizgaur.tms.entity.Bid;
import com.kshitizgaur.tms.entity.enums.BidStatus;

import lombok.*;

/**
 * DTO for Bid response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidResponseDTO {

    private UUID bidId;
    private UUID loadId;
    private UUID transporterId;
    private String transporterName;
    private Double transporterRating;
    private Double proposedRate;
    private Integer trucksOffered;
    private BidStatus status;
    private LocalDateTime submittedAt;

    /**
     * Convert Bid entity to BidResponseDTO.
     */
    public static BidResponseDTO fromEntity(Bid bid) {
        return BidResponseDTO.builder()
                .bidId(bid.getBidId())
                .loadId(bid.getLoad() != null ? bid.getLoad().getLoadId() : null)
                .transporterId(bid.getTransporter() != null ? bid.getTransporter().getTransporterId() : null)
                .transporterName(bid.getTransporter() != null ? bid.getTransporter().getCompanyName() : null)
                .transporterRating(bid.getTransporter() != null ? bid.getTransporter().getRating() : null)
                .proposedRate(bid.getProposedRate())
                .trucksOffered(bid.getTrucksOffered())
                .status(bid.getStatus())
                .submittedAt(bid.getSubmittedAt())
                .build();
    }
}
