package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * msg_export_deq$1100 out_msg:^MsgEnvelope
 * import_block_lt:uint63 = OutMsg;
 * </pre>
 */
@Builder
@Data
public class OutMsgDeq implements OutMsg, Serializable {
  int magic;
  MsgEnvelope outMsg;
  BigInteger importBlockLt;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b1100, 4)
        .storeRef(outMsg.toCell())
        .storeUint(importBlockLt, 63)
        .endCell();
  }

  public static OutMsgDeq deserialize(CellSlice cs) {
    return OutMsgDeq.builder()
        .magic(cs.loadUint(4).intValue())
        .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
        .importBlockLt(cs.loadUint(63))
        .build();
  }
}
