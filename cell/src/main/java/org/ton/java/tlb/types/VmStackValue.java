package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

public interface VmStackValue {

    Cell toCell();

    static VmStackValue deserialize(CellSlice cs) {
        int magic = cs.preloadUint(8).intValue();
        if (magic == 0x00) {
            return VmStackValueNull.deserialize(cs);
        } else if (magic == 0x01) {
            return VmStackValueTinyInt.deserialize(cs);
        } else if (magic == 0x02) {
            return VmStackValueInt.deserialize(cs);
        }
        return null;
    }
}
