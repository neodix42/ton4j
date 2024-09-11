package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 * <pre>
 * storage_used_short$_
 *   cells:(VarUInteger 7)
 *   bits:(VarUInteger 7) = StorageUsedShort;
 *   </pre>
 */
@Builder
@Data

public class StorageUsedShort {
    BigInteger cells;
    BigInteger bits;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeVarUint(cells, 3) // (VarUInteger 7)
                .storeVarUint(bits, 3)
                .endCell();
    }

    public static StorageUsedShort deserialize(CellSlice cs) {
        return StorageUsedShort.builder()
                .cells(cs.loadVarUInteger(BigInteger.valueOf(3)))
                .bits(cs.loadVarUInteger(BigInteger.valueOf(3)))
                .build();
    }
}
