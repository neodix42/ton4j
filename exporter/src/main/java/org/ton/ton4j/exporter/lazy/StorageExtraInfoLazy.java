package org.ton.ton4j.exporter.lazy;

import java.io.Serializable;
import org.ton.ton4j.cell.Cell;

public interface StorageExtraInfoLazy extends Serializable {

  Cell toCell();

  static StorageExtraInfoLazy deserialize(CellSliceLazy cs) {
    int magic = cs.preloadUint(3).intValue();
    if (magic == 0b000) {
      return StorageExtraNoneLazy.deserialize(cs);
    } else if (magic == 0b001) {
      return StorageExtraInformationLazy.deserialize(cs);

    } else {
      throw new Error("Wrong magic in StorageExtraInfo, found  " + magic + ", required 0 or 1.");
    }
  }
}
