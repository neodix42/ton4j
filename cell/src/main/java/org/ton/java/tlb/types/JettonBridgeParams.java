package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;


public interface JettonBridgeParams {

    Cell toCell();

    static JettonBridgeParams deserialize(CellSlice cs) {
        int magic = cs.preloadUint(8).intValue();
        if (magic == 0x00) {
            return JettonBridgeParamsV1.deserialize(cs);
        } else if (magic == 0x01) {
            return JettonBridgeParamsV2.deserialize(cs);
        } else {
            throw new Error("Wrong magic in JettonBridgeParams: " + magic);
        }
    }
}
