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
 * msg_import_deferred_tr$00101 in_msg:^MsgEnvelope out_msg:^MsgEnvelope = InMsg;
 *
 * </pre>
 */
@Builder
@Data
public class InMsgImportDeferredTr implements InMsg, Serializable {
  MsgEnvelope inMsg;
  MsgEnvelope outMsg;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b00101, 5)
        .storeRef(inMsg.toCell())
        .storeRef(outMsg.toCell())
        .endCell();
  }

  public static InMsgImportDeferredTr deserialize(CellSlice cs) {
    return InMsgImportDeferredTr.builder()
        .inMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
        .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
