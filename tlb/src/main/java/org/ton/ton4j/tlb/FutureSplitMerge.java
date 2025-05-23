package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 * </pre>
 * fsm_none$0 = FutureSplitMerge;
 * fsm_split$10 split_utime:uint32 interval:uint32 = FutureSplitMerge;
 * fsm_merge$11 merge_utime:uint32 interval:uint32 = FutureSplitMerge;
 * <pre>
 */
@Builder
@Data
public class FutureSplitMerge implements Serializable {
  int flag;
  long splitUTime;
  long mergeUTime;
  long interval;

  public Cell toCell() {
    if (flag == 0) {
      return CellBuilder.beginCell().storeUint(0b0, 1).endCell();

    } else if (flag == 0b10) {
      return CellBuilder.beginCell()
          .storeUint(0b10, 2)
          .storeUint(splitUTime, 32)
          .storeUint(interval, 32)
          .endCell();
    } else if (flag == 0b11) {
      return CellBuilder.beginCell()
          .storeUint(0b11, 2)
          .storeUint(mergeUTime, 32)
          .storeUint(interval, 32)
          .endCell();
    } else {
      throw new Error("Wrong flag at FutureSplitMerge, must be 0b0, 0b10 or 0b11, got " + flag);
    }
  }

  public static FutureSplitMerge deserialize(CellSlice cs) {
    int flag = cs.loadUint(1).intValue();
    if (flag == 0) {
      return FutureSplitMerge.builder().build();
    } else {
      flag = cs.loadUint(1).intValue();
      if (flag == 0) {
        return FutureSplitMerge.builder()
            .splitUTime(cs.loadUint(32).longValue())
            .interval(cs.loadUint(32).longValue())
            .build();
      } else {
        return FutureSplitMerge.builder()
            .mergeUTime(cs.loadUint(32).longValue())
            .interval(cs.loadUint(32).longValue())
            .build();
      }
    }
  }
}
