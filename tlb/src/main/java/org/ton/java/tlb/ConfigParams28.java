package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams28 {
  CatchainConfig catchainConfig;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(catchainConfig.toCell()).endCell();
  }

  public static ConfigParams28 deserialize(CellSlice cs) {
    return ConfigParams28.builder().catchainConfig(CatchainConfig.deserialize(cs)).build();
  }
}
