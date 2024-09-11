package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 * <pre>
 * import_fees$_
 * fees_collected:Grams
 * value_imported:CurrencyCollection = ImportFees;
 * </pre>
 */
@Builder
@Data
public class ImportFees {
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
