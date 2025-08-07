package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

/**
 * Response for getShardBlockProof endpoint
 */
@Data
public class ShardBlockProofResponse {
    
    @SerializedName("masterchain_id")
    private BlockIdExt masterchainId;
    
    @SerializedName("links")
    private List<ProofLink> links;
    
    @Data
    public static class BlockIdExt {
        private Integer workchain;
        private Long shard;
        private Long seqno;
        
        @SerializedName("root_hash")
        private String rootHash;
        
        @SerializedName("file_hash")
        private String fileHash;
    }
    
    @Data
    public static class ProofLink {
        private BlockIdExt id;
        private String proof;
    }
}
