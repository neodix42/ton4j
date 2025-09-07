package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

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

    // Handle empty/null dictionary entries (magic 0x0) by returning zero counters
    // This matches the original TON implementation behavior in unpack_CreatorStats()
    if (magic == 0x0) {
      return CreatorStats.builder()
          .magic(0x0)
          .mcBlocks(
              Counters.builder()
                  .lastUpdated(0L)
                  .total(BigInteger.ZERO)
                  .cnt2048(BigInteger.ZERO)
                  .cnt65536(BigInteger.ZERO)
                  .build())
          .shardBlocks(
              Counters.builder()
                  .lastUpdated(0L)
                  .total(BigInteger.ZERO)
                  .cnt2048(BigInteger.ZERO)
                  .cnt65536(BigInteger.ZERO)
                  .build())
          .build();
    }

    assert (magic == 0x4)
        : "CreatorStats: magic not equal to 0x4, found 0x" + Long.toHexString(magic);

    return CreatorStats.builder()
        .magic(magic)
        .mcBlocks(Counters.deserialize(cs))
        .shardBlocks(Counters.deserialize(cs))
        .build();
  }
}
