package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class RawAccountState {
    @SerializedName(value = "@type")
    final String type = "raw.accountState";
    String balance;
    String code;
    String data;
    LastTransactionId last_transaction_id;
    long sync_utime;
}
