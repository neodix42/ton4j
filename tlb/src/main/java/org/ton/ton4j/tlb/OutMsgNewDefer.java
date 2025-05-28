package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * msg_export_new_defer$10100 out_msg:^MsgEnvelope
 * transaction:^Transaction = OutMsg;
 * </pre>
 */
@Builder
@Data
public class OutMsgNewDefer implements OutMsg, Serializable {
  int magic;
  MsgEnvelope outMsg;
  Transaction transaction;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b10100, 5)
        .storeRef(outMsg.toCell())
        .storeRef(transaction.toCell())
        .endCell();
  }

  public static OutMsgNewDefer deserialize(CellSlice cs) {
    return OutMsgNewDefer.builder()
        .magic(cs.loadUint(5).intValue())
        .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
        .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
