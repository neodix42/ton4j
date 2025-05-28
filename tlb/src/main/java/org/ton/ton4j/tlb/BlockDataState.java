package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class BlockDataState implements Serializable {
  BlockData blockData;
  ShardState blockState;
}
