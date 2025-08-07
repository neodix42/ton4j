package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class WalletInformationResponse {
    private Boolean wallet;
    private String balance;
    
    @SerializedName("account_state")
    private String accountState;
    
    @SerializedName("wallet_type")
    private String walletType;
    
    private Integer seqno;
    
    @SerializedName("wallet_id")
    private Long walletId;
    
    @SerializedName("last_transaction_id")
    private TransactionId lastTransactionId;
    
    @Data
    public static class TransactionId {
        @SerializedName("@type")
        private String type;
        private String lt;
        private String hash;
    }
}
