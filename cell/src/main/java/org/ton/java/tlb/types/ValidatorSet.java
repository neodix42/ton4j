package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>{@code
 * validators#11 utime_since:uint32 utime_until:uint32
 * total:(## 16) main:(## 16) { main <= total } { main >= 1 }
 * list:(Hashmap 16 ValidatorDescr) = ValidatorSet;
 * validators_ext#12 utime_since:uint32 utime_until:uint32
 * total:(## 16) main:(## 16) { main <= total } { main >= 1 }
 * total_weight:uint64 list:(HashmapE 16 ValidatorDescr) = ValidatorSet;
 * }</pre>
 */
public interface ValidatorSet {
  Cell toCell();

  static ValidatorSet deserialize(CellSlice cs) {
    int magic = cs.preloadUint(8).intValue();
    if (magic == 0x53) {
      return Validators.deserialize(cs);
    } else if (magic == 0x73) {
      return ValidatorsExt.deserialize(cs);
    } else {
      throw new Error("Cannot deserialize ValidatorSet, magic: " + magic);
    }
  }
}
