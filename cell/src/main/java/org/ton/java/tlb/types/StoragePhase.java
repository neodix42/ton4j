package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * tr_phase_storage$_ storage_fees_collected:Grams
 *   storage_fees_due:(Maybe Grams)
 *   status_change:AccStatusChange
 *   = TrStoragePhase;
 */
public class StoragePhase {
    BigInteger storageFeesCollected;
    BigInteger storageFeesDue;
    AccStatusChange statusChange;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCoins(storageFeesCollected)
                .storeCoinsMaybe(storageFeesDue)
                .storeSlice(CellSlice.beginParse(((AccStatusChange) statusChange).toCell()))
                .endCell();
    }
}
