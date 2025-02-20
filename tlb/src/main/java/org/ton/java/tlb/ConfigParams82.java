package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams82 {
  JettonBridgeParams polygonTonTokenBridge;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(polygonTonTokenBridge.toCell()).endCell();
  }

  public static ConfigParams82 deserialize(CellSlice cs) {
    return ConfigParams82.builder()
        .polygonTonTokenBridge(JettonBridgeParams.deserialize(cs))
        .build();
  }
}
