package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.java.tonlib.types.AccountTransactionId;
import org.ton.java.tonlib.types.BlockIdExt;

@Builder
@Data
public class GetBlockTransactionsQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "blocks.getTransactions";

  BlockIdExt id;
  int mode;
  long count;
  AccountTransactionId after;
}
