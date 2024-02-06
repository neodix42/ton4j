package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

/**
 * block_create_stats#17 counters:(HashmapE 256 CreatorStats) = BlockCreateStats;
 */

@Builder
@Getter
@Setter
@ToString
public class BlockCreateStatsOrdinary implements BlockCreateStats {

    TonHashMapE list;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x17, 8)
                .storeDict(list.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).bits,
                        v -> CellBuilder.beginCell().storeCell(((CreatorStats) v).toCell())
                )).endCell();
    }

    public static BlockCreateStatsOrdinary deserialize(CellSlice cs) {
        return BlockCreateStatsOrdinary.builder()
                .list(cs.loadDictE(256,
                        k -> k.readInt(256),
                        v -> CreatorStats.deserialize(CellSlice.beginParse(v))))
                .build();

    }
}

