package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Response for estimateFee endpoint
 */
@Data
public class EstimateFeeResponse {
    
    @SerializedName("source_fees")
    private Fees sourceFees;
    
    @SerializedName("destination_fees")
    private Fees destinationFees;
    
    @Data
    public static class Fees {
        @SerializedName("in_fwd_fee")
        private Long inFwdFee;
        
        @SerializedName("storage_fee")
        private Long storageFee;
        
        @SerializedName("gas_fee")
        private Long gasFee;
        
        @SerializedName("fwd_fee")
        private Long fwdFee;
    }
}
