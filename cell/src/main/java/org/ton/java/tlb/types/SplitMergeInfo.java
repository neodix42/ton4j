package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
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
@Getter
@Setter
@ToString

public class SplitMergeInfo {
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
