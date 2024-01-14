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
 * trans_ord$0000
 *   credit_first:Bool
 *   storage_ph:(Maybe TrStoragePhase)
 *   credit_ph:(Maybe TrCreditPhase)
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool
 *   bounce:(Maybe TrBouncePhase)
 *   destroyed:Bool
 *   = TransactionDescr;
 */
public class TransactionDescriptionOrdinary {
    int magic;
    boolean creditFirst;
    StoragePhase storagePhase;
    CreditPhase creditPhase;
    ComputePhase computePhase;
    ActionPhase actionPhase;
    boolean aborted;
    BouncePhase bouncePhase;
    boolean destroyed;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b0000, 4)
                .storeBit(creditFirst)
                .storeCellMaybe(storagePhase.toCell())
                .storeCellMaybe(creditPhase.toCell())
                .storeCell(computePhase.toCell())
                .storeRefMaybe(actionPhase.toCell())
                .storeBit(aborted)
                .storeCellMaybe(bouncePhase.toCell())
                .storeBit(destroyed)
                .endCell();
    }
}
