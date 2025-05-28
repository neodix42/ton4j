package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 *
 *
 * <pre>
 *     blocks.transactionsExt id:ton.blockIdExt req_count:int32 incomplete:Bool transactions:vector[raw.transaction] = blocks.TransactionsExt;
 * </pre>
 */
@Builder
@Data
public class BlockTransactionsExt implements Serializable {
  @SerializedName("@type")
  final String type = "blocks.transactionsExt";

  BlockIdExt id;
  long req_count;
  boolean incomplete;
  List<RawTransaction> transactions;
}
