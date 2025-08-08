package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Response for getConsensusBlock endpoint
 */
@Data
public class ConsensusBlockResponse {
    
    @SerializedName("consensus_block")
    private Long consensusBlock;
    
    @SerializedName("timestamp")
    private Double timestamp;
}
