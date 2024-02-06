package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapAugE;

/**
 * block_create_stats_ext#34 counters:(HashmapAugE 256 CreatorStats uint32) = BlockCreateStats;
 */

@Builder
@Getter
@Setter
@ToString
public class BlockCreateStatsExt implements BlockCreateStats {

    TonHashMapAugE list;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x17, 8)
                .storeDict(list.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).bits,
                        v -> CellBuilder.beginCell().storeCell(((CreatorStats) v).toCell()),
                        e -> CellBuilder.beginCell().storeUint((Long) e, 32),
                        (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                )).endCell();
    }

    public static BlockCreateStatsExt deserialize(CellSlice cs) {
        return BlockCreateStatsExt.builder()
                .list(cs.loadDictAugE(256,
                        k -> k.readInt(256),
                        v -> CreatorStats.deserialize(CellSlice.beginParse(v)),
                        e -> cs.loadUint(32)))
                .build();

    }
}

