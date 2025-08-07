package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

@Data
public class BlockTransactionsResponse {
    @SerializedName("@type")
    private String type;
    
    private BlockIdExt id;
    
    @SerializedName("req_count")
    private Double reqCount;
    
    private Boolean incomplete;
    
    private List<RawTransaction> transactions;
    
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
    
    @Data
    public static class RawTransaction {
        @SerializedName("@type")
        private String type;
        
        private AccountAddress address;
        private Double utime;
        private String data;
        
        @SerializedName("transaction_id")
        private TransactionId transactionId;
        
        private Long fee;
        @SerializedName("storage_fee")
        private Long storageFee;
        @SerializedName("other_fee")
        private Long otherFee;
        
        @SerializedName("in_msg")
        private RawMessage inMsg;
        
        @SerializedName("out_msgs")
        private List<RawMessage> outMsgs;
        
        private String account;
    }
    
    @Data
    public static class AccountAddress {
        @SerializedName("@type")
        private String type;
        @SerializedName("account_address")
        private String accountAddress;
    }
    
    @Data
    public static class TransactionId {
        @SerializedName("@type")
        private String type;
        private String lt;
        private String hash;
    }
    
    @Data
    public static class RawMessage {
        @SerializedName("@type")
        private String type;
        private String hash;
        private AccountAddress source;
        private AccountAddress destination;
        private Long value;
        @SerializedName("extra_currencies")
        private List<Object> extraCurrencies;
        @SerializedName("fwd_fee")
        private Long fwdFee;
        @SerializedName("ihr_fee")
        private Long ihrFee;
        @SerializedName("created_lt")
        private Long createdLt;
        @SerializedName("body_hash")
        private String bodyHash;
        @SerializedName("msg_data")
        private MessageData msgData;
    }
    
    @Data
    public static class MessageData {
        @SerializedName("@type")
        private String type;
        private String body;
        @SerializedName("init_state")
        private String initState;
    }
}
