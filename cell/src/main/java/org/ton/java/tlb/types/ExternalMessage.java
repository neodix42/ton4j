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
 * ext_in_msg_info$10
 *   src:MsgAddressExt
 *   dest:MsgAddressInt
 *   import_fee:Grams
 */
public class ExternalMessage implements CommonMsgInfo {
    long magic;
    MsgAddressExt srcAddr;
    MsgAddressInt dstAddr;
    BigInteger importFee;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    public Cell toCell() {
        CellBuilder result = CellBuilder.beginCell()
                .storeUint(0b10, 2)
                .storeSlice(CellSlice.beginParse(srcAddr.toCell()))
                .storeSlice(CellSlice.beginParse(dstAddr.toCell()))
                .storeCoins(importFee);

        return result.endCell();
    }

    public static ExternalMessage deserialize(CellSlice cs) {
        long magic = cs.loadUint(2).intValue();
        assert (magic == 0b10) : "ExternalMessage: magic not equal to 0b10, found 0b" + Long.toBinaryString(magic);
        return ExternalMessage.builder()
                .magic(2L)
                .srcAddr((MsgAddressExt) MsgAddress.deserialize(cs))
                .dstAddr(MsgAddressInt.deserialize(cs))
                .importFee(cs.loadCoins())
                .build();
    }
}
