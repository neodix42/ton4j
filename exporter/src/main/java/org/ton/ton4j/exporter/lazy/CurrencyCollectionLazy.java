package org.ton.ton4j.exporter.lazy;

import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.tlb.ExtraCurrency;

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
public class CurrencyCollectionLazy implements Serializable {
  BigInteger coins;

  TonHashMapELazy extraCurrencies;

  public Cell toCell() {

    Cell dict = null;

    if (nonNull(extraCurrencies)) {
      dict =
          extraCurrencies.serialize(
              k -> CellBuilder.beginCell().storeUint((Long) k, 32).endCell().getBits(),
              v -> CellBuilder.beginCell().storeVarUint((BigInteger) v, 32).endCell());
    }
    return CellBuilder.beginCell().storeCoins(coins).storeDict(dict).endCell();
  }

  public static CurrencyCollectionLazy deserialize(CellSliceLazy cs) {
    return CurrencyCollectionLazy.builder()
        .coins(cs.loadCoins())
        .extraCurrencies(
            cs.loadDictE(
                32,
                k -> k.readUint(32).longValue(),
                v -> CellSliceLazy.beginParse(cs.cellDbReader, v).loadVarUInteger(32)))
        .build();
  }

  public List<ExtraCurrency> getExtraCurrenciesParsed() {
    ArrayList<ExtraCurrency> result = new ArrayList<>();
    for (Map.Entry<Object, Object> entry : extraCurrencies.elements.entrySet()) {
      result.add(
          ExtraCurrency.builder()
              .id((Long) entry.getKey())
              .amount((BigInteger) entry.getValue())
              .build());
    }
    return result;
  }
}
