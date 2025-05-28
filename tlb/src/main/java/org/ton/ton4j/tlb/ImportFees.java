package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;
import java.math.BigInteger;

/**
 *
 *
 * <pre>
 * import_fees$_
 * fees_collected:Grams
 * value_imported:CurrencyCollection = ImportFees;
 * </pre>
 */
@Builder
@Data
public class ImportFees implements Serializable {
  BigInteger feesCollected;
  CurrencyCollection valueImported;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeCoins(feesCollected)
        .storeCell(valueImported.toCell())
        .endCell();
  }

  public static ImportFees deserialize(CellSlice cs) {
    return ImportFees.builder()
        .feesCollected(cs.loadCoins())
        .valueImported(CurrencyCollection.deserialize(cs))
        .build();
  }
}
