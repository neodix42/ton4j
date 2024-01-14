package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
/**
 * trans_tick_tock$001
 *   is_tock:Bool
 *   storage_ph:TrStoragePhase
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool
 *   destroyed:Bool = TransactionDescr;
 */
public class TransactionDescriptionTickTock {
    int magic;
    boolean isTock;
    StoragePhase storagePhase;
    ComputePhase computePhase;
    ActionPhase actionPhase;
    boolean aborted;
    boolean destroyed;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b001, 3)
                .storeBit(isTock)
                .storeCell(storagePhase.toCell())
                .storeCell(computePhase.toCell())
                .storeRefMaybe(actionPhase.toCell())
                .storeBit(aborted)
                .storeBit(destroyed)
                .endCell();
    }
}
