package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

@Builder
@Data
public class ConfigParams12 {
  TonHashMapE workchains;

  public Cell toCell() {

    return CellBuilder.beginCell()
        .storeDict(
            workchains.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 32).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((WorkchainDescr) v).toCell()).endCell()))
        .endCell();
  }

  public static ConfigParams12 deserialize(CellSlice cs) {
    return ConfigParams12.builder()
        .workchains(
            cs.loadDictE(
                32, k -> k.readUint(32), v -> WorkchainDescr.deserialize(CellSlice.beginParse(v))))
        .build();
  }
}
