package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

public interface StorageExtraInfo {

  Cell toCell();

  static StorageExtraInfo deserialize(CellSlice cs) {
    int magic = cs.preloadUint(3).intValue();
    if (magic == 0x00) {
      return StorageExtraNone.deserialize(cs);
    } else if (magic == 0x01) {
      return StorageExtraInformation.deserialize(cs);

    } else {
      throw new Error("Wrong magic in StorageExtraInfo: " + magic);
    }
  }
}
