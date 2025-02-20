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
 * ext_out_msg_info$11
 * src:MsgAddressInt
 * dest:MsgAddressExt
 * created_lt:uint64 - default zero
 * created_at:uint32 - default zero
 * = CommonMsgInfo;
 * </pre>
 */
@Builder
@Data
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
    CellBuilder result =
        CellBuilder.beginCell()
            .storeUint(0b11, 2)
            .storeCell(
                isNull(srcAddr) ? MsgAddressExtNone.builder().build().toCell() : srcAddr.toCell())
            .storeCell(dstAddr.toCell())
            .storeUint(isNull(createdLt) ? BigInteger.ZERO : createdLt, 64)
            .storeUint(createdAt, 32);
    return result.endCell();
  }

  public static ExternalMessageOutInfo deserialize(CellSlice cs) {
    long magic = cs.loadUint(2).intValue();
    assert (magic == 0b11)
        : "ExternalMessageOut: magic not equal to 0b11, found 0b" + Long.toBinaryString(magic);
    return ExternalMessageOutInfo.builder()
        .magic(magic)
        .srcAddr(MsgAddressInt.deserialize(cs))
        .dstAddr(MsgAddressExt.deserialize(cs))
        .createdLt(cs.loadUint(64))
        .createdAt(cs.loadUint(32).longValue())
        .build();
  }
}
