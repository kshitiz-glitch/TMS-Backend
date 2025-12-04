package com.kshitizgaur.tms.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.kshitizgaur.tms.entity.Bid;
import com.kshitizgaur.tms.entity.enums.BidStatus;

import lombok.*;

/**
 * DTO for best bid response with calculated score.
 * Used in GET /load/{loadId}/best-bids endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BestBidDTO {

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
     * Calculated score for ranking.
     * Formula: score = (1 / proposedRate) * 0.7 + (rating / 5) * 0.3
     * Higher score = better bid
     */
    private Double score;

    /**
     * Convert Bid entity to BestBidDTO with calculated score.
     */
    public static BestBidDTO fromEntity(Bid bid) {
        double rating = bid.getTransporter() != null ? bid.getTransporter().getRating() : 3.0;
        double score = (1.0 / bid.getProposedRate()) * 0.7 + (rating / 5.0) * 0.3;

        return BestBidDTO.builder()
                .bidId(bid.getBidId())
                .loadId(bid.getLoad() != null ? bid.getLoad().getLoadId() : null)
                .transporterId(bid.getTransporter() != null ? bid.getTransporter().getTransporterId() : null)
                .transporterName(bid.getTransporter() != null ? bid.getTransporter().getCompanyName() : null)
                .transporterRating(rating)
                .proposedRate(bid.getProposedRate())
                .trucksOffered(bid.getTrucksOffered())
                .status(bid.getStatus())
                .submittedAt(bid.getSubmittedAt())
                .score(score)
                .build();
    }
}
