package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

public interface ComputePhase {

  Cell toCell();

  static ComputePhase deserialize(CellSlice cs) {
    boolean isNotSkipped = cs.loadBit();
    if (isNotSkipped) {
      return ComputePhaseVM.deserialize(cs);
    }
    return ComputeSkipReason.deserialize(cs);
  }
}
