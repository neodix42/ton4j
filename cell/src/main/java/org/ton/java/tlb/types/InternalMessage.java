package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

import static java.util.Objects.nonNull;

@Builder
@Getter
@Setter
@ToString
/**
 * int_msg_info$0
 *   ihr_disabled:Bool
 *   bounce:Bool
 *   bounced:Bool
 *   src:MsgAddressInt
 *   dest:MsgAddressInt
 *   value:CurrencyCollection
 *   ihr_fee:Grams
 *   fwd_fee:Grams
 *   created_lt:uint64
 *   created_at:uint32
 */
public class InternalMessage extends CommonMsg {
    long magic; // must be 0
    boolean iHRDisabled;
    boolean bounce;
    boolean bounced;
    MsgAddress srcAddr;
    MsgAddress dstAddr;
    CurrencyCollection value;
    BigInteger iHRFee;
    BigInteger fwdFee;
    BigInteger createdLt;
    Long createdAt;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        CellBuilder result = CellBuilder.beginCell()
                .storeUint(0, 1)
                .storeBit(iHRDisabled)
                .storeBit(bounce)
                .storeBit(bounced)
                .storeSlice(CellSlice.beginParse(srcAddr.toCell())) //MsgAddressInt
                .storeSlice(CellSlice.beginParse(dstAddr.toCell())) //MsgAddressInt
//                .storeAddress(isNull(dstAddr) ? null : Address.of((byte) 0x11, srcAddr.getMsgAddressInt().getWorkchainId(), srcAddr.getMsgAddressInt().address.toByteArray()))
//                .storeAddress(isNull(dstAddr) ? null : Address.of((byte) 0x11, dstAddr.getMsgAddressInt().getWorkchainId(), dstAddr.getMsgAddressInt().address.toByteArray()))
                .storeCoins(value.getCoins())
                .storeDict(nonNull(value.getExtraCurrencies()) ? value.getExtraCurrencies().serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                        v -> CellBuilder.beginCell().storeUint((byte) v, 32)) :
                        CellBuilder.beginCell().storeBit(false).endCell())
                .storeCoins(iHRFee)
                .storeCoins(fwdFee)
                .storeUint(createdLt, 64)
                .storeUint(createdAt, 32);
//
        return result.endCell();
    }

    public static InternalMessage deserialize(CellSlice cs) {
        boolean magicBool = cs.loadBit();
        assert (!magicBool) : "InternalMessage: magic not equal to 0, found 0x" + magicBool;

        return InternalMessage.builder()
                .magic(0L)
                .iHRDisabled(cs.loadBit())
                .bounce(cs.loadBit())
                .bounced(cs.loadBit())
                .srcAddr(MsgAddress.deserialize(cs))
                .dstAddr(MsgAddress.deserialize(cs))
                .value(CurrencyCollection.deserialize(cs))
                .iHRFee(cs.loadCoins())
                .fwdFee(cs.loadCoins())
                .createdLt(cs.loadUint(64))
                .createdAt(cs.loadUint(32).longValue())
                .build();
    }
}
