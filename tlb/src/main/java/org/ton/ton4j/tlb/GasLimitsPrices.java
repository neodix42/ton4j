package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

public interface GasLimitsPrices {

  Cell toCell();

  static GasLimitsPrices deserialize(CellSlice cs) {
    int magic = cs.preloadUint(8).intValue();

    if (magic == 0xdd) {
      return GasLimitsPricesOrdinary.deserialize(cs);
    } else if (magic == 0xde) {
      return GasLimitsPricesExt.deserialize(cs);
    } else if (magic == 0xd1) {
      return GasLimitsPricesPfx.deserialize(cs);
    } else {
      throw new Error("Cannot GasLimitsPrices WorkchainDescr");
    }
  }
}
