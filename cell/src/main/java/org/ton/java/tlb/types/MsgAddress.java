package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
/**
 * addr_none$00 = MsgAddressExt;
 * addr_extern$01 len:(## 9) external_address:(bits len) = MsgAddressExt;
 *
 * anycast_info$_ depth:(#<= 30) { depth >= 1 } rewrite_pfx:(bits depth) = Anycast;
 *
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
public class MsgAddress {
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
}
