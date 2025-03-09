package org.ton.java.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;

@Builder
@Data
public class ConfigParams10 {
  TonHashMap criticalParams;

  public Cell toCell() {

    Cell dict;

    dict =
        criticalParams.serialize(
            k -> CellBuilder.beginCell().storeUint((BigInteger) k, 32).endCell().getBits(),
            v -> CellBuilder.beginCell().storeBit((Boolean) v).endCell());
    return CellBuilder.beginCell().storeDict(dict).endCell();
  }

  public static ConfigParams10 deserialize(CellSlice cs) {
    return ConfigParams10.builder()
        .criticalParams(
            cs.loadDict(32, k -> k.readUint(32), v -> CellSlice.beginParse(v).loadBit()))
        .build();
  }
}
