package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ExtendedAddressInformationResponse {
    @SerializedName("@type")
    private String type;
    
    private String balance;
    private String state;
    private String code;
    private String data;
    
    @SerializedName("last_transaction_id")
    private TransactionId lastTransactionId;
    
    @SerializedName("block_id")
    private BlockId blockId;
    
    @SerializedName("sync_utime")
    private Long syncUtime;
    
    @Data
    public static class TransactionId {
        @SerializedName("@type")
        private String type;
        private String lt;
        private String hash;
    }
    
    @Data
    public static class BlockId {
        @SerializedName("@type")
        private String type;
        private Integer workchain;
        private String shard;
        private Integer seqno;
        @SerializedName("root_hash")
        private String rootHash;
        @SerializedName("file_hash")
        private String fileHash;
    }
}
