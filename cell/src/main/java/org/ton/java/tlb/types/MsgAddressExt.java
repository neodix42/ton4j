package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;


/**
 * addr_none$00 = MsgAddressExt;
 * addr_extern$01 len:(## 9) external_address:(bits len) = MsgAddressExt;
 */
public interface MsgAddressExt extends MsgAddress {

    Cell toCell();

//    static MsgAddressExt deserialize(CellSlice cs);
}
