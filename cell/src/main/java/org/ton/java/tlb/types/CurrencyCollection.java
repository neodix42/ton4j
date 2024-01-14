package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapE;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * extra_currencies$_ dict:(HashmapE 32 (VarUInteger 32)) = ExtraCurrencyCollection;
 * currencies$_ grams:Grams other:ExtraCurrencyCollection = CurrencyCollection;
 */
public class CurrencyCollection {
    BigInteger coins;
    TonHashMapE extraCurrencies;

    public Cell toCell() {
        Cell dictCell = extraCurrencies.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                v -> CellBuilder.beginCell().storeVarUint((byte) v, 32)
        );
        return CellBuilder.beginCell()
                .storeCoins(coins)
                .storeDict(dictCell)
                .endCell();
    }
}
