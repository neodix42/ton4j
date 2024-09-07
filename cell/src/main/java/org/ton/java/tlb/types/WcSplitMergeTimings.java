package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * <pre>
 * wc_split_merge_timings#0
 * split_merge_delay:uint32 split_merge_interval:uint32
 * min_split_merge_interval:uint32 max_split_merge_delay:uint32
 * = WcSplitMergeTimings;
 * </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class WcSplitMergeTimings {
    int magic;
    long splitMergeDelay;
    long splitMergeInterval;
    long minSplitMergeInterval;
    long minSplitMergeDelay;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0, 4)
                .storeUint(splitMergeDelay, 32)
                .storeUint(splitMergeInterval, 32)
                .storeUint(minSplitMergeInterval, 32)
                .storeUint(minSplitMergeDelay, 32)
                .endCell();
    }

    public static WcSplitMergeTimings deserialize(CellSlice cs) {
        return WcSplitMergeTimings.builder()
                .magic(cs.loadUint(4).intValue())
                .splitMergeDelay(cs.loadInt(32).longValue())
                .splitMergeInterval(cs.loadInt(32).longValue())
                .minSplitMergeInterval(cs.loadInt(32).longValue())
                .minSplitMergeDelay(cs.loadInt(32).longValue())
                .build();
    }
}
