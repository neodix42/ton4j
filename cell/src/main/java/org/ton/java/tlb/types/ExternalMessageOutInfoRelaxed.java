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
 ext_out_msg_info$11
 src:MsgAddress
 dest:MsgAddressExt
 created_lt:uint64
 created_at:uint32 = CommonMsgInfoRelaxed;
 */
public class ExternalMessageOutInfoRelaxed implements CommonMsgInfoRelaxed {
    long magic;
    MsgAddress srcAddr;
    MsgAddress dstAddr;
    BigInteger createdLt;
    Long createdAt;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    public Cell toCell() {
        CellBuilder result = CellBuilder.beginCell()
                .storeUint(3, 2)
                .storeSlice(CellSlice.beginParse(srcAddr.toCell()))
                .storeSlice(CellSlice.beginParse(dstAddr.toCell()))
                .storeUint(createdLt, 64)
                .storeUint(createdAt, 32);
        return result.endCell();
    }

    public static ExternalMessageOutInfoRelaxed deserialize(CellSlice cs) {
        long magic = cs.loadUint(2).intValue();
        assert (magic == 0b11) : "ExternalMessageOutInfoRelaxed: magic not equal to 0b11, found 0b" + Long.toBinaryString(magic);
        return ExternalMessageOutInfoRelaxed.builder()
                .magic(0b11)
                .srcAddr(MsgAddress.deserialize(cs))
                .dstAddr(MsgAddress.deserialize(cs))
                .createdLt(cs.loadUint(64))
                .createdAt(cs.loadUint(32).longValue())
                .build();
    }
}
