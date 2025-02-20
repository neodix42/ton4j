package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * _ key:Bool blk_ref:ExtBlkRef = KeyExtBlkRef;
 * </pre>
 */
@Builder
@Data
public class KeyExtBlkRef {
  boolean key;
  ExtBlkRef blkRef;

  public Cell toCell() {
    return CellBuilder.beginCell().storeBit(key).storeCell(blkRef.toCell()).endCell();
  }

  public static KeyExtBlkRef deserialize(CellSlice cs) {
    return KeyExtBlkRef.builder().key(cs.loadBit()).blkRef(ExtBlkRef.deserialize(cs)).build();
  }
}
