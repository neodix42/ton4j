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
 * ext_out_msg_info$11
 *   src:MsgAddressInt
 *   dest:MsgAddressExt
 *   created_lt:uint64
 *   created_at:uint32
 */
public class ExternalMessageOut extends CommonMsg {
    long magic;
    MsgAddress srcAddr;
    MsgAddress dstAddr;
    BigInteger createdLt;
    Long createdAt;
//    StateInit stateInit;    // `tlb:"maybe either . ^"`
//    Cell body;              // `tlb:"either . ^"`

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        CellBuilder result = CellBuilder.beginCell()
                .storeUint(3, 2)
                .storeSlice(CellSlice.beginParse(srcAddr.toCell()))
                .storeSlice(CellSlice.beginParse(dstAddr.toCell()))
//                .storeAddress(isNull(srcAddr) ? null : Address.of((byte) 0x11, srcAddr.getMsgAddressInt().getWorkchainId(), srcAddr.getMsgAddressInt().address.toByteArray()))
//                .storeAddress(isNull(dstAddr) ? null : Address.of((byte) 0x51, 0, dstAddr.getMsgAddressExt().externalAddress.toByteArray())) // todo review flag
                .storeUint(createdLt, 64)
                .storeUint(createdAt, 32);
//                .storeBit(nonNull(stateInit)); //maybe


//        if (nonNull(stateInit)) { //either
//            Cell stateInitCell = stateInit.toCell();
//            if ((result.bits.getFreeBits() - 2 < stateInitCell.bits.getUsedBytes()) || (result.getFreeRefs() - 1 < result.getMaxRefs())) {
//                result.storeBit(true);
//                result.storeRef(stateInitCell);
//            } else {
//                result.storeBit(false);
//                result.storeSlice(CellSlice.beginParse(stateInitCell));
//            }
//        }

//        if (nonNull(body)) { //either
//            if ((result.bits.getFreeBits() - 1 < body.bits.getUsedBytes()) || (result.getFreeRefs() < body.getMaxRefs())) {
//                result.storeBit(true);
//                result.storeRef(body);
//            } else {
//                result.storeBit(false);
//                result.storeSlice(CellSlice.beginParse(body));
//            }
//        } else {
//            result.storeBit(false);
//        }

        return result.endCell();
    }

    public static ExternalMessageOut deserialize(CellSlice cs) {
        long magic = cs.loadUint(2).intValue();
        assert (magic == 0b11) : "ExternalMessageOut: magic not equal to 0b11, found 0b" + Long.toBinaryString(magic);
        return ExternalMessageOut.builder()
                .magic(3L)
                .srcAddr(MsgAddress.deserialize(cs))
                .dstAddr(MsgAddress.deserialize(cs))
                .createdLt(cs.loadUint(64))
                .createdAt(cs.loadUint(32).longValue())
                .build();
    }
}
