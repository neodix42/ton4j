package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

import static java.util.Objects.isNull;

@Builder
@Getter
@Setter
@ToString
/**
 ext_out_msg_info$11
 src:MsgAddressInt
 dest:MsgAddressExt
 created_lt:uint64 - default zero
 created_at:uint32 - default zero
 = CommonMsgInfo;
 */
public class ExternalMessageOutInfo implements CommonMsgInfo {
    long magic;
    MsgAddressInt srcAddr;
    MsgAddressExt dstAddr;
    BigInteger createdLt;
    long createdAt;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        CellBuilder result = CellBuilder.beginCell()
                .storeUint(0b11, 2)
                .storeSlice(CellSlice.beginParse(srcAddr.toCell()))
                .storeSlice(CellSlice.beginParse(dstAddr.toCell()))
                .storeUint(isNull(createdLt) ? BigInteger.ZERO : createdLt, 64)
                .storeUint(createdAt, 32);
        return result.endCell();
    }

    public static ExternalMessageOutInfo deserialize(CellSlice cs) {
        long magic = cs.loadUint(2).intValue();
        assert (magic == 0b11) : "ExternalMessageOut: magic not equal to 0b11, found 0b" + Long.toBinaryString(magic);
        return ExternalMessageOutInfo.builder()
                .magic(magic)
                .srcAddr(MsgAddressInt.deserialize(cs))
                .dstAddr(MsgAddressExt.deserialize(cs))
                .createdLt(cs.loadUint(64))
                .createdAt(cs.loadUint(32).longValue())
                .build();
    }
}
