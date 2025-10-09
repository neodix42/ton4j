package org.ton.ton4j.exporter.lazy;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.tlb.EnqueuedMsg;

/**
 *
 *
 * <pre>
 * _ (HashmapAugE 352 EnqueuedMsg uint64) = OutMsgQueue;
 * </pre>
 */
@Builder
@Data
public class OutMsgQueueLazy {
  TonHashMapAugELazy outMsgQueue;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeCell(
            outMsgQueue.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 352).endCell().getBits(),
                v -> null,
                e -> CellBuilder.beginCell().storeUint((BigInteger) e, 64).endCell(),
                (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1)))
        .endCell();
  }

  public static OutMsgQueueLazy deserialize(CellSliceLazy cs) {
    return OutMsgQueueLazy.builder()
        .outMsgQueue(
            cs.loadDictAugE(
                352,
                k -> k.readUint(352),
                v -> v, // skip
                // EnqueuedMsg::deserialize,
                e -> e.loadUint(64)))
        //        .outMsgQueue(cs.loadDictAugE(352, k -> k.readUint(352), v -> v, e ->
        // e.loadUint(64)))
        .build();
  }

  public List<EnqueuedMsg> getOutMsgQueueAsList() {
    List<EnqueuedMsg> enqueuedMsg = new ArrayList<>();
    for (Map.Entry<Object, ValueExtra> entry : this.outMsgQueue.elements.entrySet()) {
      enqueuedMsg.add((EnqueuedMsg) entry.getValue().getValue());
    }
    return enqueuedMsg;
  }
}
