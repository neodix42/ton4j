package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * msg_export_deferred_tr$10101  out_msg:^MsgEnvelope
 * imported:^InMsg = OutMsg;
 * </pre>
 */
@Builder
@Data
@Slf4j
public class OutMsgDeferredTr implements OutMsg, Serializable {
  int magic;
  MsgEnvelope outMsg;
  InMsg imported;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b10101, 5)
        .storeRef(outMsg.toCell())
        .storeRef(imported.toCell())
        .endCell();
  }

  public static OutMsgDeferredTr deserialize(CellSlice cs) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    OutMsgDeferredTr result =
        OutMsgDeferredTr.builder()
            .magic(cs.loadUint(5).intValue())
            .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
            .imported(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
            .build();
    log.info(
        "{} deserialized in {}ms", OutMsgDeferredTr.class.getSimpleName(), stopWatch.getTime());
    return result;
  }
}
