package org.ton.java.tlb;

import static java.util.Objects.isNull;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * int_msg_info$0
 * ihr_disabled:Bool - default true
 * bounce:Bool - default true
 * bounced:Bool - default false
 * src:MsgAddressInt
 * dest:MsgAddressInt
 * value:CurrencyCollection - default zero
 * ihr_fee:Grams  - default zero
 * fwd_fee:Grams - default zero
 * created_lt:uint64 - default zero
 * created_at:uint32
 * = CommonMsgInfo;
 * </pre>
 */
@Builder
@Data
public class InternalMessageInfo implements CommonMsgInfo {
  int magic;
  Boolean iHRDisabled;
  Boolean bounce;
  Boolean bounced;
  MsgAddressInt srcAddr;
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
    CellBuilder result =
        CellBuilder.beginCell()
            .storeUint(0, 1)
            .storeBit(isNull(iHRDisabled) ? true : iHRDisabled)
            .storeBit(isNull(bounce) ? true : bounce)
            .storeBit(isNull(bounced) ? false : bounced)
            .storeCell(
                isNull(srcAddr) ? MsgAddressExtNone.builder().build().toCell() : srcAddr.toCell())
            .storeCell(dstAddr.toCell())
            .storeCell(
                isNull(value)
                    ? CurrencyCollection.builder().coins(BigInteger.ZERO).build().toCell()
                    : value.toCell())
            .storeCoins(isNull(iHRFee) ? BigInteger.ZERO : iHRFee)
            .storeCoins(isNull(fwdFee) ? BigInteger.ZERO : fwdFee)
            .storeUint(isNull(createdLt) ? BigInteger.ZERO : createdLt, 64)
            .storeUint(createdAt, 32);
    return result.endCell();
  }

  public static InternalMessageInfo deserialize(CellSlice cs) {
    int magic = cs.loadUint(1).intValue();
    assert (magic == 0b0) : "InternalMessage: magic not equal to 0b0, found " + magic;

    return InternalMessageInfo.builder()
        .magic(magic)
        .iHRDisabled(cs.loadBit())
        .bounce(cs.loadBit())
        .bounced(cs.loadBit())
        .srcAddr(MsgAddressInt.deserialize(cs))
        .dstAddr(MsgAddressInt.deserialize(cs))
        .value(CurrencyCollection.deserialize(cs))
        .iHRFee(cs.loadCoins())
        .fwdFee(cs.loadCoins())
        .createdLt(cs.loadUint(64))
        .createdAt(cs.loadUint(32).longValue())
        .build();
  }
}
