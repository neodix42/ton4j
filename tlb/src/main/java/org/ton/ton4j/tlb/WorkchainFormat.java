package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

public interface WorkchainFormat {

  Cell toCell(boolean basic);

  static WorkchainFormat deserialize(CellSlice cs, boolean basic) {
    if (basic) {
      return WorkchainFormatBasic.deserialize(cs);
    } else {
      return WorkchainFormatExt.deserialize(cs);
    }
  }
}
