package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

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
public class InternalMessageInfo implements CommonMsgInfo, Serializable {
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
            .storeBit(isNull(iHRDisabled) || iHRDisabled)
            .storeBit(isNull(bounce) || bounce)
            .storeBit(!isNull(bounced) && bounced)
            .storeCell(srcAddr.toCell())
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

  @Override
  public String getType() {
    return "int_msg_info";
  }

  @Override
  public String getSourceAddress() {
    return srcAddr.toAddress().toRaw();
  }

  @Override
  public String getDestinationAddress() {
    return dstAddr.toAddress().toRaw();
  }

  @Override
  public BigInteger getValueCoins() {
    return value.getCoins();
  }

  @Override
  public HashMap getExtraCurrencies() {
    return value.getExtraCurrencies().elements;
  }

  public BigInteger getTotalFees() {
    return iHRFee.add(fwdFee);
  }
}
