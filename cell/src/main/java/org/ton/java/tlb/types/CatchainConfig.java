package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;


public interface CatchainConfig {


    Cell toCell();

    static CatchainConfig deserialize(CellSlice cs) {
        int magic = cs.preloadUint(8).intValue();
        if (magic == 0xc1) {
            return CatchainConfigC1.deserialize(cs);
        } else if (magic == 0xc2) {
            return CatchainConfigC2.deserialize(cs);
        } else {
            throw new Error("Wrong magic in CatchainConfig: ");
        }
    }
}
