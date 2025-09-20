package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
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
 * msg_export_deq$1100 out_msg:^MsgEnvelope
 * import_block_lt:uint63 = OutMsg;
 * </pre>
 */
@Builder
@Data
@Slf4j
public class OutMsgDeq implements OutMsg, Serializable {
  int magic;
  MsgEnvelope outMsg;
  BigInteger importBlockLt;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b1100, 4)
        .storeRef(outMsg.toCell())
        .storeUint(importBlockLt, 63)
        .endCell();
  }

  public static OutMsgDeq deserialize(CellSlice cs) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    OutMsgDeq result =
        OutMsgDeq.builder()
            .magic(cs.loadUint(4).intValue())
            .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
            .importBlockLt(cs.loadUint(63))
            .build();
    log.info("{} deserialized in {}ms", OutMsgDeq.class.getSimpleName(), stopWatch.getTime());
    return result;
  }
}
