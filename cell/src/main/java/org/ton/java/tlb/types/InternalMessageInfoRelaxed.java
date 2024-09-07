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
import static java.util.Objects.nonNull;

/**
 * <pre>
 * int_msg_info$0
 * ihr_disabled:Bool - default true
 * bounce:Bool - default true
 * bounced:Bool - default false
 * src:MsgAddress
 * dest:MsgAddressInt
 * value:CurrencyCollection - default zero
 * ihr_fee:Grams  - default zero
 * fwd_fee:Grams - default zero
 * created_lt:uint64 - default zero
 * created_at:uint32 - default zero
 * = CommonMsgInfoRelaxed;
 * </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class InternalMessageInfoRelaxed implements CommonMsgInfoRelaxed {
    long magic; // must be 0
    Boolean iHRDisabled;
    Boolean bounce;
    Boolean bounced;
    MsgAddress srcAddr;
    MsgAddressInt dstAddr;
    CurrencyCollection value;
    BigInteger iHRFee;
    BigInteger fwdFee;
    BigInteger createdLt;
    long createdAt;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        CellBuilder result = CellBuilder.beginCell()
                .storeUint(0, 1)
                .storeBit(isNull(iHRDisabled) ? true : iHRDisabled)
                .storeBit(isNull(bounce) ? true : bounce)
                .storeBit(isNull(bounced) ? false : bounced)
                .storeCell(isNull(srcAddr) ? MsgAddressExtNone.builder().build().toCell() : srcAddr.toCell())
                .storeCell(dstAddr.toCell())
                .storeCoins(isNull(value) ? BigInteger.ZERO : value.getCoins())
                .storeDict((nonNull(value) && nonNull(value.getExtraCurrencies())) ? value.getExtraCurrencies().serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 32).endCell().getBits(),
                        v -> CellBuilder.beginCell().storeUint((byte) v, 32).endCell()) :
                        CellBuilder.beginCell().storeBit(false).endCell())
                .storeCoins(isNull(iHRFee) ? BigInteger.ZERO : iHRFee)
                .storeCoins(isNull(fwdFee) ? BigInteger.ZERO : fwdFee)
                .storeUint(isNull(createdLt) ? BigInteger.ZERO : createdLt, 64)
                .storeUint(createdAt, 32);
        return result.endCell();
    }

    public static InternalMessageInfoRelaxed deserialize(CellSlice cs) {
        boolean magicBool = cs.loadBit();
        assert (!magicBool) : "InternalMessageInfoRelaxed: magic not equal to 0b0, found 0x" + magicBool;

        return InternalMessageInfoRelaxed.builder()
                .magic(0L)
                .iHRDisabled(cs.loadBit())
                .bounce(cs.loadBit())
                .bounced(cs.loadBit())
                .srcAddr(MsgAddress.deserialize(cs))
                .dstAddr(MsgAddressInt.deserialize(cs))
                .value(CurrencyCollection.deserialize(cs))
                .iHRFee(cs.loadCoins())
                .fwdFee(cs.loadCoins())
                .createdLt(cs.loadUint(64))
                .createdAt(cs.loadUint(32).longValue())
                .build();
    }
}
