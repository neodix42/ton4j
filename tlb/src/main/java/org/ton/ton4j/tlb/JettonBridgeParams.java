package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

public interface JettonBridgeParams {
  Cell toCell();

  static JettonBridgeParams deserialize(CellSlice cs) {
    int magic = cs.preloadUint(8).intValue();
    if (magic == 0x00) {
      return JettonBridgeParamsV1.deserialize(cs);
    } else if (magic == 0x01) {
      return JettonBridgeParamsV2.deserialize(cs);
    } else {
      throw new Error("Wrong magic in JettonBridgeParams: " + magic);
    }
  }
}
