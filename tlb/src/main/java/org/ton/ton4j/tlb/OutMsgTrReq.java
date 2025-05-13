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
 * msg_export_tr_req$111 out_msg:^MsgEnvelope
 * imported:^InMsg = OutMsg;
 * </pre>
 */
@Builder
@Data
public class OutMsgTrReq implements OutMsg, Serializable {
  int magic;
  MsgEnvelope msg;
  InMsg imported;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b111, 3)
        .storeRef(msg.toCell())
        .storeRef(imported.toCell())
        .endCell();
  }

  public static OutMsgTrReq deserialize(CellSlice cs) {
    return OutMsgTrReq.builder()
        .magic(cs.loadUint(3).intValue())
        .msg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
        .imported(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
