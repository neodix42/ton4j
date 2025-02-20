package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams20 {
  GasLimitsPrices configMcGasPrices;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(configMcGasPrices.toCell()).endCell();
  }

  public static ConfigParams20 deserialize(CellSlice cs) {
    return ConfigParams20.builder().configMcGasPrices(GasLimitsPrices.deserialize(cs)).build();
  }
}
