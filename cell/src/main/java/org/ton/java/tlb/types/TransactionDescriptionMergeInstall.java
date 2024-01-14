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
 * trans_merge_install$0111
 *   split_info:SplitMergeInfo
 *   prepare_transaction:^Transaction
 *   storage_ph:(Maybe TrStoragePhase)
 *   credit_ph:(Maybe TrCreditPhase)
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool destroyed:Bool
 *   = TransactionDescr;
 */
public class TransactionDescriptionMergeInstall {
    int magic;
    SplitMergeInfo splitInfo;
    Transaction prepareTransaction;
    StoragePhase storagePhase;
    CreditPhase creditPhase;
    ComputePhase computePhase;
    ActionPhase actionPhase;
    boolean aborted;
    boolean destroyed;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b0111, 4)
                .storeCell(splitInfo.toCell())
                .storeRef(prepareTransaction.toCell())
                .storeCellMaybe(storagePhase.toCell())
                .storeCellMaybe(creditPhase.toCell())
                .storeCell(computePhase.toCell())
                .storeRefMaybe(actionPhase.toCell())
                .storeBit(aborted)
                .storeBit(destroyed)
                .endCell();
    }
}
