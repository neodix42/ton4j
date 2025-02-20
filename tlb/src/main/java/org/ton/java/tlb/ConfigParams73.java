package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams73 {
  OracleBridgeParams polygonBridge;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(polygonBridge.toCell()).endCell();
  }

  public static ConfigParams73 deserialize(CellSlice cs) {
    return ConfigParams73.builder().polygonBridge(OracleBridgeParams.deserialize(cs)).build();
  }
}
