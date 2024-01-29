package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
/**
 * value_flow#b8e48dfb
 *  ^[
 *   from_prev_blk:CurrencyCollection
 *   to_next_blk:CurrencyCollection
 *   imported:CurrencyCollection
 *   exported:CurrencyCollection ]
 *
 *   fees_collected:CurrencyCollection
 *
 *   ^[
 *   fees_imported:CurrencyCollection
 *   recovered:CurrencyCollection
 *   created:CurrencyCollection
 *   minted:CurrencyCollection
 *   ] = ValueFlow;
 */
public class ValueFlow {
    long magic;
    CurrencyCollection fromPrevBlk;
    CurrencyCollection toNextBlk;
    CurrencyCollection imported;
    CurrencyCollection exported;
    CurrencyCollection feesCollected;
    CurrencyCollection burned;
    CurrencyCollection feesImported;
    CurrencyCollection recovered;
    CurrencyCollection created;
    CurrencyCollection minted;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        Cell cell1 = CellBuilder.beginCell()
                .storeCell(fromPrevBlk.toCell())
                .storeCell(toNextBlk.toCell())
                .storeCell(imported.toCell())
                .storeCell(exported.toCell())
                .endCell();

        Cell cell2 = CellBuilder.beginCell()
                .storeCell(feesImported.toCell())
                .storeCell(recovered.toCell())
                .storeCell(created.toCell())
                .storeCell(minted.toCell())
                .endCell();

        return CellBuilder.beginCell()
                .storeUint(0xb8e48dfbL, 32)
                .storeRef(cell1)
                .storeCell(feesCollected.toCell())
                .storeRef(cell2)
                .endCell();
    }
}
