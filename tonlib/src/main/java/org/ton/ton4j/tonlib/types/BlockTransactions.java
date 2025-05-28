package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BlockTransactions implements Serializable {
  @SerializedName("@type")
  final String type = "blocks.transactions";

  BlockIdExt id;
  long req_count;
  boolean incomplete;
  List<ShortTxId> transactions;
}
