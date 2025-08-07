package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Common response classes for various endpoints
 */
public class CommonResponses {

    /**
     * Response for getOutMsgQueueSizes endpoint
     */
    @Data
    public static class OutMsgQueueSizesResponse {
        @SerializedName("ext_msg_queue_size_limit")
        private Long extMsgQueueSizeLimit;
        
        @SerializedName("shards")
        private List<ShardQueueSize> shards;
        
        @Data
        public static class ShardQueueSize {
            private Integer workchain;
            private Long shard;
            private Long size;
        }
    }

    /**
     * Response for getConfigParam endpoint
     */
    @Data
    public static class ConfigParamResponse {
        @SerializedName("config")
        private Map<String, Object> config;
    }

    /**
     * Response for getConfigAll endpoint
     */
    @Data
    public static class ConfigAllResponse {
        @SerializedName("config")
        private Map<String, Object> config;
        
        @SerializedName("config_proof")
        private String configProof;
    }

    /**
     * Response for tryLocateTx, tryLocateResultTx, tryLocateSourceTx endpoints
     */
    @Data
    public static class LocateTxResponse {
        @SerializedName("transaction")
        private TransactionResponse transaction;
    }

    /**
     * Response for sendQuery endpoint
     */
    @Data
    public static class SendQueryResponse {
        @SerializedName("code")
        private Integer code;
        
        @SerializedName("hash")
        private String hash;
    }
}
