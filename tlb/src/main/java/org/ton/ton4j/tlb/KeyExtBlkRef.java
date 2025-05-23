package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * _ key:Bool blk_ref:ExtBlkRef = KeyExtBlkRef;
 * </pre>
 */
@Builder
@Data
public class KeyExtBlkRef implements Serializable {
  boolean key;
  ExtBlkRef blkRef;

  public Cell toCell() {
    return CellBuilder.beginCell().storeBit(key).storeCell(blkRef.toCell()).endCell();
  }

  public static KeyExtBlkRef deserialize(CellSlice cs) {
    return KeyExtBlkRef.builder().key(cs.loadBit()).blkRef(ExtBlkRef.deserialize(cs)).build();
  }
}
