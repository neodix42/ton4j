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
public class ExternalMessage extends CommonMsg {
    long magic; //0b10
    MsgAddress srcAddr;
    MsgAddress dstAddr;
    BigInteger importFee;
//    StateInit stateInit;    // `tlb:"maybe either . ^"`
//    Cell body;              // `tlb:"either . ^"`

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        CellBuilder result = CellBuilder.beginCell()
                .storeUint(magic, 2)
                .storeSlice(CellSlice.beginParse(srcAddr.toCell()))
                .storeSlice(CellSlice.beginParse(dstAddr.toCell()))
//                .storeAddress(isNull(srcAddr) ? null : Address.of((byte) 0x51, 0, srcAddr.getMsgAddressExt().externalAddress.toByteArray())) // todo review flag
//                .storeAddress(isNull(dstAddr) ? null : Address.of((byte) 0x11, dstAddr.getMsgAddressInt().getWorkchainId(), dstAddr.getMsgAddressInt().address.toByteArray()))
                .storeCoins(importFee);
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
//
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
}
