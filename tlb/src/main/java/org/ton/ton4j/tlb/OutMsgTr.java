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
 * msg_export_tr$011  out_msg:^MsgEnvelope
 * imported:^InMsg = OutMsg;
 * </pre>
 */
@Builder
@Data
@Slf4j
public class OutMsgTr implements OutMsg, Serializable {
  int magic;
  MsgEnvelope outMsg;
  InMsg imported;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b011, 3)
        .storeRef(outMsg.toCell())
        .storeRef(imported.toCell())
        .endCell();
  }

  public static OutMsgTr deserialize(CellSlice cs) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    OutMsgTr result =
        OutMsgTr.builder()
            .magic(cs.loadUint(3).intValue())
            .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
            .imported(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
            .build();
    log.info("{} deserialized in {}ms", OutMsgTr.class.getSimpleName(), stopWatch.getTime());
    return result;
  }
}
