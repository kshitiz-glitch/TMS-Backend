package com.kshitizgaur.tms.dto.response;

import java.util.List;

import lombok.*;

/**
 * DTO for Load with its active bids.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadWithBidsDTO {

    private LoadResponseDTO load;
    private List<BidResponseDTO> activeBids;
    private Integer remainingTrucks;
}
