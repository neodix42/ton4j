package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

import static java.util.Objects.isNull;

/**
 * ext_in_msg_info$10
 * src:MsgAddressExt
 * dest:MsgAddressInt
 * import_fee:Grams - default zero
 * = CommonMsgInfo;
 * <p>
 * import_fee - default BigInteger.ZERO
 */
@Builder
@Data

public class ExternalMessageInfo implements CommonMsgInfo {
    long magic;
    MsgAddressExt srcAddr;
    MsgAddressInt dstAddr;
    BigInteger importFee;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        CellBuilder result = CellBuilder.beginCell()
                .storeUint(0b10, 2)
                .storeCell(isNull(srcAddr) ? MsgAddressExtNone.builder().build().toCell() : srcAddr.toCell())
                .storeCell(dstAddr.toCell())
                .storeCoins(isNull(importFee) ? BigInteger.ZERO : importFee);
        return result.endCell();
    }

    public static ExternalMessageInfo deserialize(CellSlice cs) {
        long magic = cs.loadUint(2).intValue();
        assert (magic == 0b10) : "ExternalMessage: magic not equal to 0b10, found 0b" + Long.toBinaryString(magic);
        return ExternalMessageInfo.builder()
                .magic(magic)
                .srcAddr(MsgAddressExt.deserialize(cs))
                .dstAddr(MsgAddressInt.deserialize(cs))
                .importFee(cs.loadCoins())
                .build();
    }
}
