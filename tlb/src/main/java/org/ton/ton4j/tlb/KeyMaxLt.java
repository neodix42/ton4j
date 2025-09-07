package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** _ key:Bool max_end_lt:uint64 = KeyMaxLt; */
@Builder
@Data
public class KeyMaxLt implements Serializable {
  boolean key;
  BigInteger maxEndLt;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeBit(key)
        .storeUint(maxEndLt, 64)
        .endCell();
  }

  public static KeyMaxLt deserialize(CellSlice cs) {
    return KeyMaxLt.builder()
        .key(cs.loadBit())
        .maxEndLt(cs.loadUint(64))
        .build();
  }
}
