package org.ton.java.tlb;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

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
