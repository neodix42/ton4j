package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * wc_split_merge_timings#0
 * split_merge_delay:uint32 split_merge_interval:uint32
 * min_split_merge_interval:uint32 max_split_merge_delay:uint32
 * = WcSplitMergeTimings;
 * </pre>
 */
@Builder
@Data
public class WcSplitMergeTimings implements Serializable {
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
