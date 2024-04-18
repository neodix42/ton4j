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
/**
 * out_list_empty$_ = OutList 0;
 * out_list$_ {n:#} prev:^(OutList n) action:OutAction = OutList (n + 1);
 */
public class OutList {
    OutList prev;
    OutAction action;

    public Cell toCell(int n) {
        if (n == 0) {
            return CellBuilder.beginCell().endCell();
        } else {
            return CellBuilder.beginCell()
                    .storeRef(prev.toCell(n - 1)) // todo ?
                    .storeCell(action.toCell())
                    .endCell();
        }
    }

    public static OutList deserialize(CellSlice cs, int n) {
        if (n == 0) {
            return OutList.builder().build();
        } else {
            return OutList.builder()
                    .prev(OutList.deserialize(CellSlice.beginParse(cs.loadRef()), n))
                    .action(OutAction.deserialize(cs))
                    .build();
        }
    }
}
