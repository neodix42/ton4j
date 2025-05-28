package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

public interface WorkchainDescr {
  Cell toCell();

  static WorkchainDescr deserialize(CellSlice cs) {
    int magic = cs.preloadUint(8).intValue();

    if (magic == 0xa6) {
      return WorkchainDescrV1.deserialize(cs);
    } else if (magic == 0xa7) {
      return WorkchainDescrV2.deserialize(cs);
    } else {
      throw new Error("Cannot deserialize WorkchainDescr");
    }
  }
}
