package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams8 {
  GlobalVersion globalVersion;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(globalVersion.toCell()).endCell();
  }

  public static ConfigParams8 deserialize(CellSlice cs) {
    return ConfigParams8.builder().globalVersion(GlobalVersion.deserialize(cs)).build();
  }
}
