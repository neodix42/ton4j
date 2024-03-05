package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.tonlib.types.AccountAddressOnly;
import org.ton.java.tonlib.types.LastTransactionId;

@Builder
@Setter
@Getter
@ToString
public class GetRawTransactionsV2Query {
    @SerializedName(value = "@type")
    final String type = "raw.getTransactionsV2";
    AccountAddressOnly account_address;
    LastTransactionId from_transaction_id;
    int count;
    boolean try_decode_message;
}