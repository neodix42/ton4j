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
 * msg_export_new$001 out_msg:^MsgEnvelope
 * transaction:^Transaction = OutMsg;
 * </pre>
 */
@Builder
@Data
@Slf4j
public class OutMsgNew implements OutMsg, Serializable {
  int magic;
  MsgEnvelope outMsg;
  Transaction transaction;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b001, 3)
        .storeRef(outMsg.toCell())
        .storeRef(transaction.toCell())
        .endCell();
  }

  public static OutMsgNew deserialize(CellSlice cs) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    OutMsgNew result =
        OutMsgNew.builder()
            .magic(cs.loadUint(3).intValue())
            .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
            .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
            .build();
    log.info("{} deserialized in {}ms", OutMsgNew.class.getSimpleName(), stopWatch.getTime());
    return result;
  }
}
