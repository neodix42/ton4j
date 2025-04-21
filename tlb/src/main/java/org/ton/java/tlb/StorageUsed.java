package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * storage_used$_
 *   cells:(VarUInteger 7)
 *   bits:(VarUInteger 7)
 *   public_cells:(VarUInteger 7) = StorageUsed;
 *   </pre>
 */
@Builder
@Data
public class StorageUsed implements Serializable {
  BigInteger bitsUsed;
  BigInteger cellsUsed;
  BigInteger publicCellsUsed;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeVarUint(cellsUsed, 3)
        .storeVarUint(bitsUsed, 3)
        .storeVarUint(publicCellsUsed, 3)
        .endCell();
  }

  public static StorageUsed deserialize(CellSlice cs) {
    return StorageUsed.builder()
        .cellsUsed(cs.loadVarUInteger(BigInteger.valueOf(3)))
        .bitsUsed(cs.loadVarUInteger(BigInteger.valueOf(3)))
        .publicCellsUsed(cs.loadVarUInteger(BigInteger.valueOf(3)))
        .build();
  }
}
