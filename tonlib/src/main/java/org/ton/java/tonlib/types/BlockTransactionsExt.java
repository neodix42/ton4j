package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 *
 * <pre>
 *     blocks.transactionsExt id:ton.blockIdExt req_count:int32 incomplete:Bool transactions:vector[raw.transaction] = blocks.TransactionsExt;
 * </pre>
 */
@Builder
@Setter
@Getter
@ToString
public class BlockTransactionsExt implements Serializable {
  @SerializedName("@type")
  final String type = "blocks.transactionsExt";

  BlockIdExt id;
  long req_count;
  boolean incomplete;
  List<RawTransaction> transactions;
}
