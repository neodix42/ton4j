package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BlockDataState {
  BlockData blockData;
  ShardState blockState;
}
