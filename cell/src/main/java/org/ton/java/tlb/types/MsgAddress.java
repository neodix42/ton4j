package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * addr_none$00 = MsgAddressExt;
 * addr_extern$01 len:(## 9) external_address:(bits len) = MsgAddressExt;
 * anycast_info$_ depth:(#<= 30) { depth >= 1 } rewrite_pfx:(bits depth) = Anycast;
 * addr_std$10 anycast:(Maybe Anycast)  workchain_id:int8 address:bits256  = MsgAddressInt;
 *
 * addr_var$11
 *   anycast:(Maybe Anycast)
 *   addr_len:(## 9)
 *   workchain_id:int32
 *   address:(bits addr_len) = MsgAddressInt;
 *
 * _ _:MsgAddressInt = MsgAddress;
 * _ _:MsgAddressExt = MsgAddress;
 */
public class MsgAddress { // todo interface?
    int magic;
    MsgAddressInt msgAddressInt;
    MsgAddressExt msgAddressExt;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    public Cell toCell() {
        switch (magic) {
            case 0b00 -> {
                return CellBuilder.beginCell().storeUint(0, 2).endCell();
            }
            case 0b01 -> {
                return msgAddressExt.toCell();
            }
            case 0b10 -> {
                return ((MsgAddressIntStd) msgAddressInt).toCell();
            }
            case 0b11 -> {
                return ((MsgAddressIntVar) msgAddressInt).toCell();
            }
        }
        throw new Error("wrong magic number");
    }

    public static MsgAddress deserialize(CellSlice cs) {
        MsgAddressExt extMsgAddr = null;
        MsgAddressInt intMsgAddr = null;
        int flagMsg = cs.loadUint(2).intValue();
        switch (flagMsg) {
            case 0b00 -> {
                extMsgAddr = MsgAddressExt.builder().build();
            }
            case 0b01 -> {
                int len = cs.loadUint(9).intValue();
                BigInteger externalAddress = cs.loadUint(len);
                extMsgAddr = MsgAddressExt.builder()
                        .len(len)
                        .externalAddress(externalAddress)
                        .build();
            }
            case 0b10 -> {
                intMsgAddr = MsgAddressIntStd.deserialize(cs);
            }
            case 0b11 -> {
                intMsgAddr = MsgAddressIntVar.deserialize(cs);
            }
        }
        return MsgAddress.builder()
                .magic(flagMsg)
                .msgAddressExt(extMsgAddr)
                .msgAddressInt(intMsgAddr)
                .build();
    }
}
