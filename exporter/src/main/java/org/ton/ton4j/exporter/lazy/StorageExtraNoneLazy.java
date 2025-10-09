package org.ton.ton4j.exporter.lazy;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;

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
public class StorageExtraNoneLazy implements StorageExtraInfoLazy, Serializable {
  int magic;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0, 3).endCell();
  }

  public static StorageExtraNoneLazy deserialize(CellSliceLazy cs) {
    return StorageExtraNoneLazy.builder().magic(cs.loadUint(3).intValue()).build();
  }
}
