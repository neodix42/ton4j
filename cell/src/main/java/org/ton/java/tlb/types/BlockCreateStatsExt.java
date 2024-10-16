package org.ton.java.tlb.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapAugE;

/**
 *
 *
 * <pre>
 * block_create_stats_ext#34 counters:(HashmapAugE 256 CreatorStats uint32) = BlockCreateStats;
 * </pre>
 */
@Builder
@Data
public class BlockCreateStatsExt implements BlockCreateStats {

  TonHashMapAugE counters;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x17, 8)
        .storeDict(
            counters.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((CreatorStats) v).toCell()),
                e -> CellBuilder.beginCell().storeUint((Long) e, 32),
                (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                ))
        .endCell();
  }

  public static BlockCreateStatsExt deserialize(CellSlice cs) {
    return BlockCreateStatsExt.builder()
        .counters(
            cs.loadDictAugE(
                256,
                k -> k.readInt(256),
                v -> CreatorStats.deserialize(CellSlice.beginParse(v)),
                e -> cs.loadUint(32)))
        .build();
  }

  public List<CreatorStats> getCreatorStatsAsList() {
    List<CreatorStats> creatorStats = new ArrayList<>();
    for (Map.Entry<Object, Pair<Object, Object>> entry : counters.elements.entrySet()) {
      creatorStats.add((CreatorStats) entry.getValue().getLeft());
    }
    return creatorStats;
  }
}
