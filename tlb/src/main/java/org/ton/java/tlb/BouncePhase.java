package org.ton.java.tlb;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

public interface BouncePhase {
  //    Object phase; // `tlb:"."`

  Cell toCell();

  static BouncePhase deserialize(CellSlice cs) {
    boolean isOk = cs.preloadBit();
    if (isOk) {
      return BouncePhaseOk.deserialize(cs);
    }
    boolean isNoFunds = cs.preloadBit();
    if (isNoFunds) {
      return BouncePhaseNoFounds.deserialize(cs);
    }
    return BouncePhaseNegFounds.deserialize(cs);
  }
}
