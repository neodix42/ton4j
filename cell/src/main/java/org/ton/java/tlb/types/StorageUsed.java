package org.ton.java.tlb.types;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * storage_used$_
 *   cells:(VarUInteger 7)
 *   bits:(VarUInteger 7)
 *   public_cells:(VarUInteger 7) = StorageUsed;
 */
public class StorageUsed {
    BigInteger bitsUsed;
    BigInteger cellsUsed;
    BigInteger publicCellsUsed;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeVarUint(bitsUsed, 7)
                .storeVarUint(cellsUsed, 7)
                .storeVarUint(publicCellsUsed, 7)
                .endCell();
    }
}
