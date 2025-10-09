package org.ton.ton4j.exporter.lazy;

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
public class OutMsgQueueInfoLazy implements Serializable {
  OutMsgQueueLazy outMsgQueue;
  ProcessedInfoLazy processedInfo;
  OutMsgQueueExtraLazy extra;

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

  public static OutMsgQueueInfoLazy deserialize(CellSliceLazy cs) {
    OutMsgQueueInfoLazy outMsgQueueInfo =
        OutMsgQueueInfoLazy.builder()
            .outMsgQueue(OutMsgQueueLazy.deserialize(cs))
            .processedInfo(ProcessedInfoLazy.deserialize(cs))
            .build();
    if (cs.loadBit()) {
      outMsgQueueInfo.setExtra(OutMsgQueueExtraLazy.deserialize(cs));
    }
    return outMsgQueueInfo;
  }
}
