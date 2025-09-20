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
 * msg_export_ext$000 msg:^(Message Any)
 * transaction:^Transaction = OutMsg;
 * </pre>
 */
@Builder
@Data
@Slf4j
public class OutMsgExt implements OutMsg, Serializable {
  int magic;
  Message msg;
  Transaction transaction;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b000, 3)
        .storeRef(msg.toCell())
        .storeRef(transaction.toCell())
        .endCell();
  }

  public static OutMsgExt deserialize(CellSlice cs) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    OutMsgExt result =
        OutMsgExt.builder()
            .magic(cs.loadUint(3).intValue())
            .msg(Message.deserialize(CellSlice.beginParse(cs.loadRef())))
            .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
            .build();
    log.info("{} deserialized in {}ms", OutMsgExt.class.getSimpleName(), stopWatch.getTime());
    return result;
  }
}
