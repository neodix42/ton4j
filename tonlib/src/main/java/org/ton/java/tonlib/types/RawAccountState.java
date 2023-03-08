package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Setter
@Getter
@ToString
public class RawAccountState implements Serializable {
    @SerializedName(value = "@type")
    final String type = "raw.fullAccountState";
    String balance;
    String code;
    String data;
    LastTransactionId last_transaction_id;
    BlockIdExt block_id;
    String frozen_hash;
    long sync_utime;
}
