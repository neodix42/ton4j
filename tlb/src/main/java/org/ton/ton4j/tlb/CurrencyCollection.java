package org.ton.ton4j.tlb;

import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

/**
 *
 *
 * <pre>
 * extra_currencies$_ dict:(HashmapE 32 (VarUInteger 32)) = ExtraCurrencyCollection;
 * currencies$_ grams:Grams other:ExtraCurrencyCollection = CurrencyCollection;
 * </pre>
 */
@Builder
@Data
public class CurrencyCollection implements Serializable {
  BigInteger coins;
  TonHashMapE extraCurrencies;

  public Cell toCell() {

    Cell dict = null;

    if (nonNull(extraCurrencies)) {
      dict =
          extraCurrencies.serialize(
              k -> CellBuilder.beginCell().storeUint((Long) k, 32).endCell().getBits(),
              v -> CellBuilder.beginCell().storeVarUint((BigInteger) v, 5).endCell());
    }
    return CellBuilder.beginCell().storeCoins(coins).storeDict(dict).endCell();
  }

  public static CurrencyCollection deserialize(CellSlice cs) {
    return CurrencyCollection.builder()
        .coins(cs.loadCoins())
        .extraCurrencies(
            cs.loadDictE(
                32,
                k -> k.readUint(32).longValue(),
                v -> CellSlice.beginParse(v).loadVarUInteger(BigInteger.valueOf(5))))
        .build();
  }
}
