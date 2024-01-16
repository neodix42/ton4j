package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapAugE;
import org.ton.java.cell.TonHashMapE;

@Builder
@Getter
@Setter
@ToString
/**
 // _ (HashmapAugE 352 EnqueuedMsg uint64) = OutMsgQueue;
 // _ (HashmapE 96 ProcessedUpto) = ProcessedInfo;
 // _ (HashmapE 320 IhrPendingSince) = IhrPendingInfo;
 */

public class OutMsgQueueInfo {
    TonHashMapAugE outMsgQueue;
    TonHashMapE processedInfo;
    TonHashMapE ihrPendingInfo;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeDict(outMsgQueue.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 352).bits,
                        v -> CellBuilder.beginCell().storeCell(((EnqueuedMsg) v).toCell()),
                        e -> CellBuilder.beginCell().storeUint((Long) e, 64).bits))
                .storeDict(processedInfo.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 96).bits,
                        v -> CellBuilder.beginCell().storeCell(((ProcessedUpto) v).toCell())))
                .storeDict(ihrPendingInfo.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 320).bits,
                        v -> CellBuilder.beginCell().storeUint((Long) v, 64).endCell()))
                .endCell();
    }
}
