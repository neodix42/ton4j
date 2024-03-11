package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

public interface WorkchainFormat {

    Cell toCell(boolean basic);

    static WorkchainFormat deserialize(CellSlice cs, boolean basic) {
        if (basic) {
            return WorkchainFormatBasic.deserialize(cs);
        } else {
            return WorkchainFormatExt.deserialize(cs);
        }
    }
}
