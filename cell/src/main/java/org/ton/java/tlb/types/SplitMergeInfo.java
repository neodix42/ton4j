package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * split_merge_info$_
 *  cur_shard_pfx_len:(## 6)
 *  acc_split_depth:(## 6)
 *  this_addr:bits256
 *  sibling_addr:bits256
 *   = SplitMergeInfo;
 */
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
}
