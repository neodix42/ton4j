package org.ton.ton4j.exporter.lazy;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;

/**
 *
 *
 * <pre>
 * storage_used$_
 *   cells:(VarUInteger 7)
 *   bits:(VarUInteger 7) = StorageUsed;
 *   </pre>
 */
@Builder
@Data
public class StorageUsedLazy implements Serializable {
  BigInteger cellsUsed;
  BigInteger bitsUsed;

  public Cell toCell() {
    return CellBuilder.beginCell().storeVarUint(cellsUsed, 7).storeVarUint(bitsUsed, 7).endCell();
  }

  public static StorageUsedLazy deserialize(CellSliceLazy cs) {
    return StorageUsedLazy.builder()
        .cellsUsed(cs.loadVarUInteger(7))
        .bitsUsed(cs.loadVarUInteger(7))
        .build();
  }
}
