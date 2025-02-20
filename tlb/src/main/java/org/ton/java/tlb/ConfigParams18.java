package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;

@Builder
@Data
public class ConfigParams18 {
  TonHashMap storagePrices;

  public Cell toCell() {

    Cell dict;

    dict =
        storagePrices.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, 32).endCell().getBits(),
            v -> CellBuilder.beginCell().storeCell(((StoragePrices) v).toCell()).endCell());
    return CellBuilder.beginCell().storeDict(dict).endCell();
  }

  public static ConfigParams18 deserialize(CellSlice cs) {
    return ConfigParams18.builder()
        .storagePrices(cs.loadDict(32, k -> k.readUint(32), v -> StoragePrices.deserialize(cs)))
        .build();
  }
}
