package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * storage_extra_none$000 = StorageExtraInfo;
 *
 *
 *   </pre>
 */
@Builder
@Data
public class StorageExtraNone implements StorageExtraInfo, Serializable {
  int magic;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0, 3).endCell();
  }

  public static StorageExtraNone deserialize(CellSlice cs) {
    return StorageExtraNone.builder().magic(cs.loadUint(3).intValue()).build();
  }
}
