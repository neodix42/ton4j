package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

public interface ComputePhase {
    //Object phase; // `tlb:"."`

    public Cell toCell();

    public static ComputePhase deserialize(CellSlice cs) {
        boolean isNotSkipped = cs.loadBit();
        if (isNotSkipped) {
            return ComputePhaseVM.deserialize(cs);
        }
        return ComputeSkipReason.deserialize(cs);
    }

}
