package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

public interface VmStackValue {

    Cell toCell();

    static VmStackValue deserialize(CellSlice cs) {
        CellSlice c = cs.clone();
        int magic = c.preloadUint(8).intValue();
        int magic2 = c.skipBits(8).preloadUint(8).intValue();
        if (magic == 0x00) {
            return VmStackValueNull.deserialize(cs);
        } else if (magic == 0x01) {
            return VmStackValueTinyInt.deserialize(cs);
        } else if (magic == 0x02) {
            return VmStackValueInt.deserialize(cs);

//            if (magic2 == 0x01) {
//                return VmStackValueInt.deserialize(cs);
//            } else if (magic2 == 0xff) {
//                return VmStackValueNaN.deserialize(cs);
//            } else {
//                throw new Error("Error deserializing VmStackValue, wrong magic " + magic2);
//            }
        } else if (magic == 0x03) {
            return VmStackValueCell.deserialize(cs);
        } else if (magic == 0x04) {
            return VmStackValueSlice.deserialize(cs);
        } else if (magic == 0x05) {
            return VmStackValueBuilder.deserialize(cs);
        } else if (magic == 0x06) {
            return VmStackValueCont.deserialize(cs);
        } else if (magic == 0x07) {
            return VmStackValueCont.deserialize(cs);
        } else {
            throw new Error("Error deserializing VmStackValue, wrong magic " + magic);
        }
    }
}
