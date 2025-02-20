package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * msg_export_deq_imm$100 out_msg:^MsgEnvelope
 * reimport:^InMsg = OutMsg;
 * </pre>
 */
@Builder
@Data
public class OutMsgDeqImm implements OutMsg {
  int magic;
  MsgEnvelope msg;
  InMsg reimport;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b100, 3)
        .storeRef(msg.toCell())
        .storeRef(reimport.toCell())
        .endCell();
  }

  public static OutMsgDeqImm deserialize(CellSlice cs) {
    return OutMsgDeqImm.builder()
        .magic(cs.loadUint(3).intValue())
        .msg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
        .reimport(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
