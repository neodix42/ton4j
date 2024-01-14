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
 * trans_split_prepare$0100
 *   split_info:SplitMergeInfo
 *   storage_ph:(Maybe TrStoragePhase)
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool destroyed:Bool
 *   = TransactionDescr;
 */
public class TransactionDescriptionSplitPrepare {
    int magic;
    SplitMergeInfo splitInfo;
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
                .storeUint(0b0100, 4)
                .storeCell(splitInfo.toCell())
                .storeCellMaybe(storagePhase.toCell())
                .storeCell(computePhase.toCell())
                .storeRefMaybe(actionPhase.toCell())
                .storeBit(aborted)
                .storeBit(destroyed)
                .endCell();
    }
}
