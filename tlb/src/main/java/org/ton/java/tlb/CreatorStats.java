package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * creator_info#4 mc_blocks:Counters shard_blocks:Counters = CreatorStats;
 * </pre>
 */
@Builder
@Data
public class CreatorStats implements Serializable {

  long magic;
  Counters mcBlocks;
  Counters shardBlocks;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x4, 4)
        .storeCell(mcBlocks.toCell())
        .storeCell(shardBlocks.toCell())
        .endCell();
  }

  public static CreatorStats deserialize(CellSlice cs) {
    long magic = cs.loadUint(4).longValue();
    assert (magic == 0x4)
        : "CreatorStats: magic not equal to 0x4, found 0x" + Long.toHexString(magic);

    return CreatorStats.builder()
        .magic(0x4)
        .mcBlocks(Counters.deserialize(cs))
        .shardBlocks(Counters.deserialize(cs))
        .build();
  }
}
