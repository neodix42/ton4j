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
 * split_merge_info$_
 *  cur_shard_pfx_len:(## 6)
 *  acc_split_depth:(## 6)
 *  this_addr:bits256
 *  sibling_addr:bits256
 *   = SplitMergeInfo;
 *   </pre>
 */
@Builder
@Data
public class SplitMergeInfo implements Serializable {
  int curShardPfxLen;
  int accSplitDepth;
  BigInteger thisAddr;
  BigInteger siblingAddr;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(curShardPfxLen, 6)
        .storeUint(accSplitDepth, 6)
        .storeUint(thisAddr, 256)
        .storeUint(siblingAddr, 256)
        .endCell();
  }

  public static SplitMergeInfo deserialize(CellSlice cs) {
    return SplitMergeInfo.builder()
        .curShardPfxLen(cs.loadUint(6).intValue())
        .accSplitDepth(cs.loadUint(6).intValue())
        .thisAddr(cs.loadUint(256))
        .siblingAddr(cs.loadUint(256))
        .build();
  }
}
