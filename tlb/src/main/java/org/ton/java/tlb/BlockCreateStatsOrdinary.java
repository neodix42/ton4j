package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

/**
 *
 *
 * <pre>
 * block_create_stats#17 counters:(HashmapE 256 CreatorStats) = BlockCreateStats;
 * </pre>
 */
@Builder
@Data
public class BlockCreateStatsOrdinary implements BlockCreateStats {

  TonHashMapE list;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x17, 8)
        .storeDict(
            list.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((CreatorStats) v).toCell()).endCell()))
        .endCell();
  }

  public static BlockCreateStatsOrdinary deserialize(CellSlice cs) {
    return BlockCreateStatsOrdinary.builder()
        .list(
            cs.loadDictE(
                256, k -> k.readInt(256), v -> CreatorStats.deserialize(CellSlice.beginParse(v))))
        .build();
  }
}
