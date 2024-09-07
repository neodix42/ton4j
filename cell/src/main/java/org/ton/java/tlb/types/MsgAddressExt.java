package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;


/**
 * <pre>
 * addr_none$00 = MsgAddressExt;
 * addr_extern$01 len:(## 9) external_address:(bits len) = MsgAddressExt;
 * </pre>
 */
public interface MsgAddressExt extends MsgAddress {

    String toString();

    Cell toCell();

    static MsgAddressExt deserialize(CellSlice cs) {
        int magic = cs.preloadUint(2).intValue();

        if (magic == 0b00) {
            return MsgAddressExtNone.deserialize(cs);
        } else if (magic == 0b01) {
            return MsgAddressExternal.deserialize(cs);
        } else {
            throw new Error("Wrong magic for MsgAddressExt, found " + magic);
        }
    }
}
