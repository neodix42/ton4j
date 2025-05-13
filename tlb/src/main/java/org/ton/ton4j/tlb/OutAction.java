package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

public interface OutAction {

  Cell toCell();

  static OutAction deserialize(CellSlice cs) {
    long magic = cs.preloadUint(32).longValue();
    if (magic == 0x0ec3c86dL) {
      return ActionSendMsg.deserialize(cs);
    } else if (magic == 0xad4de08eL) { // negative long
      return ActionSetCode.deserialize(cs);
    } else if (magic == 0x36e6b809L) {
      return ActionReserveCurrency.deserialize(cs);
    } else {
      throw new Error("wrong magic for OutAction: " + magic);
    }
  }
}
