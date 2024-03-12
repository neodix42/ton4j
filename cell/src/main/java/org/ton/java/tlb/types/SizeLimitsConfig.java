package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;


public interface SizeLimitsConfig {

    Cell toCell();

    static SizeLimitsConfig deserialize(CellSlice cs) {
        int magic = cs.preloadUint(8).intValue();
        if (magic == 0x01) {
            return SizeLimitsConfigV1.deserialize(cs);
        } else if (magic == 0x02) {
            return SizeLimitsConfigV2.deserialize(cs);

        } else {
            throw new Error("Wrong magic in SizeLimitsConfig: " + magic);
        }
    }
}
