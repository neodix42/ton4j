package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

@Data
public class BlockHeaderResponse {
    @SerializedName("@type")
    private String type;
    
    private BlockIdExt id;
    
    @SerializedName("global_id")
    private Double globalId;
    
    private Double version;
    private Double flags;
    
    @SerializedName("after_merge")
    private Boolean afterMerge;
    
    @SerializedName("after_split")
    private Boolean afterSplit;
    
    @SerializedName("before_split")
    private Boolean beforeSplit;
    
    @SerializedName("want_merge")
    private Boolean wantMerge;
    
    @SerializedName("want_split")
    private Boolean wantSplit;
    
    @SerializedName("validator_list_hash_short")
    private Double validatorListHashShort;
    
    @SerializedName("catchain_seqno")
    private Double catchainSeqno;
    
    @SerializedName("min_ref_mc_seqno")
    private Double minRefMcSeqno;
    
    @SerializedName("is_key_block")
    private Boolean isKeyBlock;
    
    @SerializedName("prev_key_block_seqno")
    private Double prevKeyBlockSeqno;
    
    @SerializedName("start_lt")
    private Long startLt;
    
    @SerializedName("end_lt")
    private Long endLt;
    
    @SerializedName("gen_utime")
    private Double genUtime;
    
    @SerializedName("prev_blocks")
    private List<BlockIdExt> prevBlocks;
    
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
