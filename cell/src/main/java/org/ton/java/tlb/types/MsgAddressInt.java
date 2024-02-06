package org.ton.java.tlb.types;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;


/**
 * addr_std$10 anycast:(Maybe Anycast)  workchain_id:int8 address:bits256  = MsgAddressInt;
 * <p>
 * addr_var$11
 * anycast:(Maybe Anycast)
 * addr_len:(## 9)
 * workchain_id:int32
 * address:(bits addr_len) = MsgAddressInt;
 */
public interface MsgAddressInt {
    public Cell toCell();

    public Address toAddress();

    public static MsgAddressInt deserialize(CellSlice cs) {
        MsgAddressInt intMsgAddr = null;
        int flagMsg = cs.loadUint(2).intValue();
        switch (flagMsg) {
            case 0b10 -> {
                intMsgAddr = MsgAddressIntStd.deserialize(cs);
            }
            case 0b11 -> {
                intMsgAddr = MsgAddressIntVar.deserialize(cs);
            }
        }
        return intMsgAddr;
    }
}
