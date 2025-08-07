package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 * Response model for getShards API call
 */
@Data
public class ShardsResponse {
    
    @SerializedName("@type")
    private String type;
    
    private List<BlockIdExt> shards;
    
    @Data
    public static class BlockIdExt {
        @SerializedName("@type")
        private String type;
        
        private Double workchain;
        private Long shard;
        private Double seqno;
        
        @SerializedName("root_hash")
        private String rootHash;
        
        @SerializedName("file_hash")
        private String fileHash;
    }
}
