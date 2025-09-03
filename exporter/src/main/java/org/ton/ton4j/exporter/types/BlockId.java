package org.ton.ton4j.exporter.types;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BlockId implements Serializable {
  int workchain;
  long shard;
  long seqno;

  public String getShard() {
    return Long.toUnsignedString(shard, 16);
  }
}
