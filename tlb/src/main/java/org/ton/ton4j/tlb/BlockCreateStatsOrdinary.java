package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

/**
 *
 *
 * <pre>
 * block_create_stats#17 counters:(HashmapE 256 CreatorStats) = BlockCreateStats;
 * </pre>
 */
@Builder
@Data
public class BlockCreateStatsOrdinary implements BlockCreateStats, Serializable {
  long magic;
  TonHashMapE list;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x17, 8)
        .storeDict(
            list.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((CreatorStats) v).toCell()).endCell()))
        .endCell();
  }

  public static BlockCreateStatsOrdinary deserialize(CellSlice cs) {
    long magic = cs.loadUint(8).longValue();
    assert (magic == 0x17) : "Block: magic not equal to 0x17, found 0x" + Long.toHexString(magic);
    return BlockCreateStatsOrdinary.builder()
        .magic(magic)
        .list(
            cs.loadDictE(
                256, k -> k.readUint(256), v -> CreatorStats.deserialize(CellSlice.beginParse(v))))
        .build();
  }
}
