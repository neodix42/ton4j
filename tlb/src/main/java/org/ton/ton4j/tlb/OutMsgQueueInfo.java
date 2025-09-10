package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.*;

/**
 *
 *
 * <pre> *
 * out_queue:OutMsgQueue proc_info:ProcessedInfo extra:(Maybe OutMsgQueueExtra) = OutMsgQueueInfo;
 * </pre>
 */
@Builder
@Data
public class OutMsgQueueInfo implements Serializable {
  OutMsgQueue outMsgQueue;
  ProcessedInfo processedInfo;
  OutMsgQueueExtra extra;

  public Cell toCell() {
    CellBuilder cellBuilder =
        CellBuilder.beginCell().storeCell(outMsgQueue.toCell()).storeCell(processedInfo.toCell());
    if (isNull(extra)) {
      cellBuilder.storeBit(false);
    } else {
      cellBuilder.storeBit(true);
      cellBuilder.storeCell(extra.toCell());
    }
    return cellBuilder.endCell();
  }

  public static OutMsgQueueInfo deserialize(CellSlice cs) {
    OutMsgQueueInfo outMsgQueueInfo =
        OutMsgQueueInfo.builder()
            .outMsgQueue(OutMsgQueue.deserialize(cs))
            .processedInfo(ProcessedInfo.deserialize(cs))
            .build();
    if (cs.loadBit()) {
      outMsgQueueInfo.setExtra(OutMsgQueueExtra.deserialize(cs));
    }
    return outMsgQueueInfo;
  }
}
