package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

public interface AccountState {
  Cell toCell();

  static AccountState deserialize(CellSlice cs) {
    boolean isActive = cs.preloadBit();
    if (isActive) {
      return AccountStateActive.deserialize(cs);
    } else {

      boolean isStatusFrozen = cs.preloadBitAt(2);
      if (isStatusFrozen) {
        return AccountStateFrozen.deserialize(cs);
      } else {
        return AccountStateUninit.deserialize(cs);
      }
    }
  }
}
