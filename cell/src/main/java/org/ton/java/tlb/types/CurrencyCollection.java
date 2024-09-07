package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

import java.math.BigInteger;

import static java.util.Objects.isNull;

/**
 * <pre>
 * extra_currencies$_ dict:(HashmapE 32 (VarUInteger 32)) = ExtraCurrencyCollection;
 * currencies$_ grams:Grams other:ExtraCurrencyCollection = CurrencyCollection;
 * </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class CurrencyCollection {
    BigInteger coins;
    TonHashMapE extraCurrencies;

    public Cell toCell() {

        Cell dict;

        if (isNull(extraCurrencies)) {
            dict = CellBuilder.beginCell().storeBit(false).endCell();
        } else {
            dict = extraCurrencies.serialize(
                    k -> CellBuilder.beginCell().storeUint((Long) k, 32).endCell().getBits(),
                    v -> CellBuilder.beginCell().storeVarUint((byte) v, 5).endCell()
            );
        }
        return CellBuilder.beginCell()
                .storeCoins(coins)
                .storeDict(dict)
                .endCell();
    }

    public static CurrencyCollection deserialize(CellSlice cs) {
        return CurrencyCollection.builder()
                .coins(cs.loadCoins())
                .extraCurrencies(cs.loadDictE(32,
                        k -> k.readUint(32),
                        v -> CellSlice.beginParse(v).loadVarUInteger(BigInteger.valueOf(5))))
                .build();
    }

}
