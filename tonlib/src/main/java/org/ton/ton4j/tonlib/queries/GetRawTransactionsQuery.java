package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tonlib.types.AccountAddressOnly;
import org.ton.ton4j.tonlib.types.LastTransactionId;

@Builder
@Data
public class GetRawTransactionsQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "raw.getTransactions";

  AccountAddressOnly account_address;
  LastTransactionId from_transaction_id;
}
