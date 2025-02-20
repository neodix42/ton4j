package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams71 {
  OracleBridgeParams ethereumBridge;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(ethereumBridge.toCell()).endCell();
  }

  public static ConfigParams71 deserialize(CellSlice cs) {
    return ConfigParams71.builder().ethereumBridge(OracleBridgeParams.deserialize(cs)).build();
  }
}
