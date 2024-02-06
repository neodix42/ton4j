package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
@ToString
/**
 creator_info#4 mc_blocks:Counters shard_blocks:Counters = CreatorStats;
 */
public class CreatorStats {

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
        assert (magic == 0x4) : "CreatorStats: magic not equal to 0x4, found 0x" + Long.toHexString(magic);

        return CreatorStats.builder()
                .magic(0x4)
                .mcBlocks(Counters.deserialize(cs))
                .shardBlocks(Counters.deserialize(cs))
                .build();
    }
}
