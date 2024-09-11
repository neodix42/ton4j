package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * <pre>
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
 *   </pre>
 */
@Builder
@Data
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

    public static ValueFlow deserialize(CellSlice cs) {
        long magic = cs.loadUint(32).longValue();
        if (magic == 0xb8e48dfbL) {

            CellSlice c1 = CellSlice.beginParse(cs.loadRef());
            CellSlice c2 = CellSlice.beginParse(cs.loadRef());

            CurrencyCollection fromPrevBlk = CurrencyCollection.deserialize(c1);
            CurrencyCollection toNextBlk = CurrencyCollection.deserialize(c1);
            CurrencyCollection imported = CurrencyCollection.deserialize(c1);
            CurrencyCollection exported = CurrencyCollection.deserialize(c1);

            CurrencyCollection feesCollected = CurrencyCollection.deserialize(cs);

            CurrencyCollection feesImported = CurrencyCollection.deserialize(c2);
            CurrencyCollection recovered = CurrencyCollection.deserialize(c2);
            CurrencyCollection created = CurrencyCollection.deserialize(c2);
            CurrencyCollection minted = CurrencyCollection.deserialize(c2);

            return ValueFlow.builder()
                    .magic(0xb8e48dfbL)
                    .fromPrevBlk(fromPrevBlk)
                    .toNextBlk(toNextBlk)
                    .imported(imported)
                    .exported(exported)
                    .feesCollected(feesCollected)
                    .feesImported(feesImported)
                    .recovered(recovered)
                    .created(created)
                    .minted(minted)
                    .build();
        }
        if (magic == 0x3ebf98b7L) {
            CellSlice c1 = CellSlice.beginParse(cs.loadRef());
            CellSlice c2 = CellSlice.beginParse(cs.loadRef());

            CurrencyCollection fromPrevBlk = CurrencyCollection.deserialize(c1);
            CurrencyCollection toNextBlk = CurrencyCollection.deserialize(c1);
            CurrencyCollection imported = CurrencyCollection.deserialize(c1);
            CurrencyCollection exported = CurrencyCollection.deserialize(c1);

            CurrencyCollection feesCollected = CurrencyCollection.deserialize(cs);
            CurrencyCollection burned = CurrencyCollection.deserialize(cs);

            CurrencyCollection feesImported = CurrencyCollection.deserialize(c2);
            CurrencyCollection recovered = CurrencyCollection.deserialize(c2);
            CurrencyCollection created = CurrencyCollection.deserialize(c2);
            CurrencyCollection minted = CurrencyCollection.deserialize(c2);

            return ValueFlow.builder()
                    .magic(0xb8e48dfbL)
                    .fromPrevBlk(fromPrevBlk)
                    .toNextBlk(toNextBlk)
                    .imported(imported)
                    .exported(exported)
                    .feesCollected(feesCollected)
                    .burned(burned)
                    .feesImported(feesImported)
                    .recovered(recovered)
                    .created(created)
                    .minted(minted)
                    .build();
        } else {
            throw new Error("ValueFlow: magic not equal to 0xb8e48dfb, found 0x" + Long.toHexString(magic));
        }
    }
}
