package org.ton.java.tlb;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

public interface ConsensusConfig {

  Cell toCell();

  static ConsensusConfig deserialize(CellSlice cs) {
    int magic = cs.preloadUint(8).intValue();
    if (magic == 0xd6) {
      return ConsensusConfigV1.deserialize(cs);
    } else if (magic == 0xd7) {
      return ConsensusConfigNew.deserialize(cs);
    } else if (magic == 0xd8) {
      return ConsensusConfigV3.deserialize(cs);
    } else if (magic == 0xd9) {
      return ConsensusConfigV4.deserialize(cs);
    } else {
      throw new Error("Wrong magic in ConsensusConfig: " + magic);
    }
  }
}
