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
 * msg_export_deq_imm$100 out_msg:^MsgEnvelope
 * reimport:^InMsg = OutMsg;
 * </pre>
 */
@Builder
@Data
@Slf4j
public class OutMsgDeqImm implements OutMsg, Serializable {
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
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    OutMsgDeqImm result =
        OutMsgDeqImm.builder()
            .magic(cs.loadUint(3).intValue())
            .msg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
            .reimport(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
            .build();
    log.info("{} deserialized in {}ms", OutMsgDeqImm.class.getSimpleName(), stopWatch.getTime());
    return result;
  }
}
