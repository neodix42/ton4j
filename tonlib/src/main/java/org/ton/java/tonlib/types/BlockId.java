package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BlockId implements Serializable {
  @SerializedName(value = "@type")
  final String type = "ton.blockId";

  long workchain;
  long shard;
  long seqno;
}
