package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
public class RawTransactions implements Serializable {
    @SerializedName("@type")
    final String type = "raw.transactions";
    List<RawTransaction> transactions;
    LastTransactionId previous_transaction_id;
}