package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** action_send_msg#0ec3c86d mode:(## 8) out_msg:^(MessageRelaxed Any) = OutAction; */
@Builder
@Data
public class ActionSendMsg implements OutAction {
  long magic;
  int mode;
  MessageRelaxed outMsg;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x0ec3c86d, 32)
        .storeUint(mode, 8)
        .storeRef(outMsg.toCell())
        .endCell();
  }

  public static ActionSendMsg deserialize(CellSlice cs) {
    return ActionSendMsg.builder()
        .magic(cs.loadUint(32).intValue())
        .mode(cs.loadUint(8).intValue())
        .outMsg(MessageRelaxed.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
