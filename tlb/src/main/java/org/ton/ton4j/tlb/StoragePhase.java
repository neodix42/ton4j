package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * tr_phase_storage$_
 *   storage_fees_collected:Grams
 *   storage_fees_due:(Maybe Grams)
 *   status_change:AccStatusChange
 *   = TrStoragePhase;
 *   </pre>
 */
@Builder
@Data
public class StoragePhase implements Serializable {
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
