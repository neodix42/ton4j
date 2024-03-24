package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 * addr_none$00 = MsgAddressExt;
 * addr_extern$01 len:(## 9) external_address:(bits len) = MsgAddressExt;
 * anycast_info$_ depth:(#<= 30) { depth >= 1 } rewrite_pfx:(bits depth) = Anycast;
 * addr_std$10 anycast:(Maybe Anycast)  workchain_id:int8 address:bits256  = MsgAddressInt;
 * <p>
 * addr_var$11
 * anycast:(Maybe Anycast)
 * addr_len:(## 9)
 * workchain_id:int32
 * address:(bits addr_len) = MsgAddressInt;
 * <p>
 * _ _:MsgAddressInt = MsgAddress;
 * _ _:MsgAddressExt = MsgAddress;
 */
public interface MsgAddress {

    Cell toCell();

    static MsgAddress deserialize(CellSlice cs) {

        int flagMsg = cs.loadUint(2).intValue();
        switch (flagMsg) {
            case 0b00 -> {
                return MsgAddressExtNone.builder().build();
            }
            case 0b01 -> {
                int len = cs.loadUint(9).intValue();
                BigInteger externalAddress = cs.loadUint(len);
                return MsgAddressExtern.builder()
                        .len(len)
                        .externalAddress(externalAddress)
                        .build();
            }
            case 0b10 -> {
                return MsgAddressIntStd.deserialize(cs);
            }
            case 0b11 -> {
                return MsgAddressIntVar.deserialize(cs);
            }
        }
        throw new Error("cannot deserialize MsgAddress");
    }
}
