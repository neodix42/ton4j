package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

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
