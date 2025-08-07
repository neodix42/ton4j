package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Response for lookupBlock endpoint
 */
@Data
public class LookupBlockResponse {
    
    private Integer workchain;
    private Long shard;
    private Long seqno;
    
    @SerializedName("root_hash")
    private String rootHash;
    
    @SerializedName("file_hash")
    private String fileHash;
}
