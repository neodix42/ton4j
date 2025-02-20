package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams72 {
  OracleBridgeParams binanceSmartChainBridge;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(binanceSmartChainBridge.toCell()).endCell();
  }

  public static ConfigParams72 deserialize(CellSlice cs) {
    return ConfigParams72.builder()
        .binanceSmartChainBridge(OracleBridgeParams.deserialize(cs))
        .build();
  }
}
