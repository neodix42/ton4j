package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

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
public class StorageUsed implements Serializable {
  BigInteger cellsUsed;
  BigInteger bitsUsed;

  public Cell toCell() {
    return CellBuilder.beginCell().storeVarUint(cellsUsed, 3).storeVarUint(bitsUsed, 3).endCell();
  }

  public static StorageUsed deserialize(CellSlice cs) {
    return StorageUsed.builder()
        .cellsUsed(cs.loadVarUInteger(BigInteger.valueOf(3)))
        .bitsUsed(cs.loadVarUInteger(BigInteger.valueOf(3)))
        .build();
  }
}
