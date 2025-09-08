package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

import java.math.BigInteger;

public interface BlockLimits {
  Cell toCell();

  static BlockLimits deserialize(CellSlice cs) {
    BigInteger magic = cs.preloadUint(8);
    if (magic.intValue() == 0x5d) {
      return BlockLimitsV1.deserialize(cs);
    }
    if (magic.intValue() == 0x5e) {
      return BlockLimitsV2.deserialize(cs);
    } else {
      throw new Error("unsupported magic, supported 0x5d or 0x5e, found " + magic.toString(16));
    }
  }
}
