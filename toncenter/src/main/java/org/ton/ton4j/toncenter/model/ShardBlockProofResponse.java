package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

/**
 * Response for getShardBlockProof endpoint
 */
@Data
public class ShardBlockProofResponse {
    
    @SerializedName("from")
    private BlockIdExt from;
    
    @SerializedName("mc_id")
    private BlockIdExt masterchainId;
    
    @SerializedName("links")
    private List<ProofLink> links;
    
    @SerializedName("mc_proof")
    private List<BlockLinkBack> mcProof;
    
    @Data
    public static class BlockIdExt {
        @SerializedName("@type")
        private String type;
        
        private Integer workchain;
        private String shard;  // Changed to String as it can be "-9223372036854775808"
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
    
    @Data
    public static class BlockLinkBack {
        @SerializedName("@type")
        private String type;
        
        @SerializedName("to_key_block")
        private Boolean toKeyBlock;
        
        @SerializedName("from")
        private BlockIdExt from;
        
        @SerializedName("to")
        private BlockIdExt to;
        
        @SerializedName("dest_proof")
        private String destProof;
        
        @SerializedName("proof")
        private String proof;
        
        @SerializedName("state_proof")
        private String stateProof;
    }
}
