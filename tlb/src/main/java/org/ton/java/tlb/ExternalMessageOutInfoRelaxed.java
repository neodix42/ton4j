package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.io.Serializable;
import java.math.BigInteger;

import static java.util.Objects.isNull;

/**
 *
 *
 * <pre>
 * ext_out_msg_info$11
 * src:MsgAddress
 * dest:MsgAddressExt
 * created_lt:uint64 - default zero
 * created_at:uint32 = default zero
 * = CommonMsgInfoRelaxed;
 * </pre>
 */
@Builder
@Data
public class ExternalMessageOutInfoRelaxed implements CommonMsgInfoRelaxed, Serializable {
  long magic;
  MsgAddress srcAddr;
  MsgAddressExt dstAddr;
  BigInteger createdLt;
  long createdAt;

  private String getMagic() {
    return Long.toBinaryString(magic);
  }

  public Cell toCell() {
    CellBuilder result =
        CellBuilder.beginCell()
            .storeUint(3, 2)
            .storeCell(
                isNull(srcAddr) ? MsgAddressExtNone.builder().build().toCell() : srcAddr.toCell())
            .storeCell(dstAddr.toCell())
            .storeUint(isNull(createdLt) ? BigInteger.ZERO : createdLt, 64)
            .storeUint(createdAt, 32);
    return result.endCell();
  }

  public static ExternalMessageOutInfoRelaxed deserialize(CellSlice cs) {
    long magic = cs.loadUint(2).intValue();
    assert (magic == 0b11)
        : "ExternalMessageOutInfoRelaxed: magic not equal to 0b11, found 0b"
            + Long.toBinaryString(magic);
    return ExternalMessageOutInfoRelaxed.builder()
        .magic(0b11)
        .srcAddr(MsgAddress.deserialize(cs))
        .dstAddr(MsgAddressExt.deserialize(cs))
        .createdLt(cs.loadUint(64))
        .createdAt(cs.loadUint(32).longValue())
        .build();
  }
}
