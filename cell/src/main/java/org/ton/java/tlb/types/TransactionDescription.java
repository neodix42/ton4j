package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
@ToString
public class TransactionDescription {
    Object description; // `tlb:"."`

    public Cell toCell() {
        CellBuilder c = CellBuilder.beginCell();

        if (description instanceof TransactionDescriptionStorage) {
            c.storeUint(0b0001, 3);
            c.storeSlice(CellSlice.beginParse(((TransactionDescriptionStorage) description).toCell()));
        } else if (description instanceof TransactionDescriptionOrdinary) {
            c.storeUint(0b000, 3);
            c.storeSlice(CellSlice.beginParse(((TransactionDescriptionOrdinary) description).toCell()));
        } else if (description instanceof TransactionDescriptionOrdinary) {
            c.storeUint(0b000, 3);
            c.storeSlice(CellSlice.beginParse(((TransactionDescriptionOrdinary) description).toCell()));
        }
        return c.endCell();
    }
}
