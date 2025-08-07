package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Response model for getAddressInformation API endpoint
 */
@Data
public class AddressInformationResponse {
    
    @SerializedName("balance")
    private String balance;
    
    @SerializedName("code")
    private String code;
    
    @SerializedName("data")
    private String data;
    
    @SerializedName("last_transaction_id")
    private LastTransactionId lastTransactionId;
    
    @SerializedName("block_id")
    private BlockId blockId;
    
    @SerializedName("frozen_hash")
    private String frozenHash;
    
    @SerializedName("sync_utime")
    private Long syncUtime;
    
    @Data
    public static class LastTransactionId {
        @SerializedName("@type")
        private String type;
        
        @SerializedName("lt")
        private String lt;
        
        @SerializedName("hash")
        private String hash;
    }
    
    @Data
    public static class BlockId {
        @SerializedName("@type")
        private String type;
        
        @SerializedName("workchain")
        private Integer workchain;
        
        @SerializedName("shard")
        private String shard;
        
        @SerializedName("seqno")
        private Long seqno;
        
        @SerializedName("root_hash")
        private String rootHash;
        
        @SerializedName("file_hash")
        private String fileHash;
    }
}
