package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 * <pre>
 * tr_phase_storage$_ storage_fees_collected:Grams
 *   storage_fees_due:(Maybe Grams)
 *   status_change:AccStatusChange
 *   = TrStoragePhase;
 *   </pre>
 */
@Builder
@Data

public class StoragePhase {
    BigInteger storageFeesCollected;
    BigInteger storageFeesDue;
    AccStatusChange statusChange;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCoins(storageFeesCollected)
                .storeCoinsMaybe(storageFeesDue)
                .storeSlice(CellSlice.beginParse(statusChange.toCell()))
                .endCell();
    }

    public static StoragePhase deserialize(CellSlice cs) {
        return StoragePhase.builder()
                .storageFeesCollected(cs.loadCoins())
                .storageFeesDue(cs.loadBit() ? cs.loadCoins() : null)
                .statusChange(AccStatusChange.deserialize(cs))
                .build();
    }
}
