package org.ton.java.tlb;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

/** _ to_mint:ExtraCurrencyCollection = ConfigParam 7; */
@Builder
@Data
public class ConfigParams7 implements Serializable {
  TonHashMapE extraCurrencies;

  public Cell toCell() {

    Cell dict;

    if (isNull(extraCurrencies)) {
      dict = CellBuilder.beginCell().storeBit(false).endCell();
    } else {
      dict =
          extraCurrencies.serialize( // dict:(HashmapE 32 (VarUInteger 32))
              k -> CellBuilder.beginCell().storeUint((BigInteger) k, 32).endCell().getBits(),
              v -> CellBuilder.beginCell().storeVarUint((byte) v, 5).endCell());
    }
    return CellBuilder.beginCell().storeDict(dict).endCell();
  }

  public static ConfigParams7 deserialize(CellSlice cs) {
    return ConfigParams7.builder()
        .extraCurrencies(
            cs.loadDictE(
                32,
                k -> k.readUint(32),
                v -> CellSlice.beginParse(v).loadVarUInteger(BigInteger.valueOf(5))))
        .build();
  }
}
