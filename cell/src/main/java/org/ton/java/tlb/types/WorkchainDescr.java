package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

public interface WorkchainDescr {
    Cell toCell();

    static WorkchainDescr deserialize(CellSlice cs) {
        int magic = cs.loadUint(8).intValue();

        if (magic == 0xa6) {
            return WorkchainDescrV1.deserialize(cs);
        } else if (magic == 0xa7) {
            return WorkchainDescrV2.deserialize(cs);
        } else {
            throw new Error("Cannot deserialize WorkchainDescr");
        }
    }
}
