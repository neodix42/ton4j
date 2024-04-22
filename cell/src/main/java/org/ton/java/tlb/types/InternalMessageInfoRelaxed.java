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
 int_msg_info$0
 ihr_disabled:Bool
 bounce:Bool
 bounced:Bool
 src:MsgAddress
 dest:MsgAddressInt
 value:CurrencyCollection
 ihr_fee:Grams
 fwd_fee:Grams
 created_lt:uint64
 created_at:uint32 = CommonMsgInfoRelaxed;
 */
public class InternalMessageInfoRelaxed implements CommonMsgInfoRelaxed {
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
    long createdAt;

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
                .storeCoins(value.getCoins())
                .storeDict(nonNull(value.getExtraCurrencies()) ? value.getExtraCurrencies().serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                        v -> CellBuilder.beginCell().storeUint((byte) v, 32)) :
                        CellBuilder.beginCell().storeBit(false).endCell())
                .storeCoins(iHRFee)
                .storeCoins(fwdFee)
                .storeUint(createdLt, 64)
                .storeUint(createdAt, 32);
        return result.endCell();
    }

    public static InternalMessageInfoRelaxed deserialize(CellSlice cs) {
        boolean magicBool = cs.loadBit();
        assert (!magicBool) : "InternalMessageInfoRelaxed: magic not equal to 0, found 0x" + magicBool;

        return InternalMessageInfoRelaxed.builder()
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
