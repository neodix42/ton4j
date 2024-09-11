package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class BlkPrevInfo {
    ExtBlkRef prev1;
    ExtBlkRef prev2; // pointer  https://github.com/xssnick/tonutils-go/blob/46dbf5f820af066ab10c5639a508b4295e5aa0fb/tlb/block.go#L136

    public Cell toCell(boolean afterMerge) {
        if (!afterMerge) {
            return CellBuilder.beginCell()
                    .storeCell(prev1.toCell())
                    .endCell();
        } else {
            return CellBuilder.beginCell()
                    .storeRef(prev1.toCell())
                    .storeRef(prev2.toCell())
                    .endCell();
        }
    }

    public static BlkPrevInfo deserialize(CellSlice cs) {
        return null;
    }
}
