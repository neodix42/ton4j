package org.ton.java.tlb;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

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
