package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

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
