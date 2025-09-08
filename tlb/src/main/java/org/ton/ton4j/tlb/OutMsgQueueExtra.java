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
 *
 * out_msg_queue_extra#0 dispatch_queue:DispatchQueue out_queue_size:(Maybe uint48) = OutMsgQueueExtra;
 * </pre>
 */
@Builder
@Data
public class OutMsgQueueExtra implements Serializable {
  long magic;
  DispatchQueue dispatchQueue;
  Long outQueueSize;

  public Cell toCell() {
    CellBuilder result = CellBuilder.beginCell().storeUint(0, 4).storeCell(dispatchQueue.toCell());

    if (isNull(outQueueSize)) {
      result.storeBit(false);
    } else {
      result.storeBit(true);
      result.storeUint(outQueueSize, 48);
    }
    return result.endCell();
  }

  public static OutMsgQueueExtra deserialize(CellSlice cs) {
    long magic = cs.loadUint(4).longValue();
    assert (magic == 0)
        : "OutMsgQueueExtra: magic not equal to 0, found 0x" + Long.toHexString(magic);
    return OutMsgQueueExtra.builder()
        .magic(magic)
        .dispatchQueue(DispatchQueue.deserialize(cs))
        .outQueueSize(cs.loadBit() ? cs.loadUint(48).longValue() : null)
        .build();
  }
}
