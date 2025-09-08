package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.math.BigInteger;
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
  TonHashMapAugE outMsgQueue;
  TonHashMapE processedInfo;
  OutMsgQueueExtra extra;

  public Cell toCell() {
    CellBuilder cellBuilder =
        CellBuilder.beginCell()
            .storeDict(
                outMsgQueue.serialize(
                    k -> CellBuilder.beginCell().storeUint((BigInteger) k, 352).endCell().getBits(),
                    v -> CellBuilder.beginCell().storeCell(((EnqueuedMsg) v).toCell()),
                    e -> CellBuilder.beginCell().storeUint((BigInteger) e, 64).endCell().getBits(),
                    (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1))) // todo
            .storeDict(
                processedInfo.serialize(
                    k -> CellBuilder.beginCell().storeUint((BigInteger) k, 96).endCell().getBits(),
                    v ->
                        CellBuilder.beginCell().storeCell(((ProcessedUpto) v).toCell()).endCell()));
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
            .outMsgQueue(
                cs.loadDictAugE(
                    352,
                    k -> k.readUint(352),
                    EnqueuedMsg::deserialize,
                    e -> CellSlice.beginParse(e).loadUint(64)))
            .processedInfo(
                cs.loadDictE(
                    96,
                    k -> k.readUint(96),
                    v -> ProcessedUpto.deserialize(CellSlice.beginParse(v))))
            .build();
    if (cs.loadBit()) {
      outMsgQueueInfo.setExtra(OutMsgQueueExtra.deserialize(CellSlice.beginParse(cs)));
    }
    return outMsgQueueInfo;
  }
}
