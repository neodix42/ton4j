package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Response model for getMasterchainInfo endpoint
 */
@Data
public class MasterchainInfoResponse {
    
    @SerializedName("@type")
    private String type;
    
    @SerializedName("last")
    private BlockId last;
    
    @SerializedName("state_root_hash")
    private String stateRootHash;
    
    @SerializedName("init")
    private BlockId init;
    
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
