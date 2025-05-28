package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** msg_export_imm$010 out_msg:^MsgEnvelope transaction:^Transaction reimport:^InMsg = OutMsg; */
@Builder
@Data
public class OutMsgImm implements OutMsg, Serializable {
  int magic;
  MsgEnvelope msg;
  Transaction transaction;
  InMsg reimport;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b010, 3)
        .storeRef(msg.toCell())
        .storeRef(transaction.toCell())
        .storeRef(reimport.toCell())
        .endCell();
  }

  public static OutMsgImm deserialize(CellSlice cs) {
    return OutMsgImm.builder()
        .magic(cs.loadUint(3).intValue())
        .msg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
        .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
        .reimport(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
