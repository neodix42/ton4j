package org.ton.java.tlb;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * <pre>
 * out_list_empty$_ = OutList 0;
 * out_list$_ {n:#} prev:^(OutList n) action:OutAction = OutList (n + 1);
 * </pre>
 */
@Builder
@Data
public class OutList {
    List<OutAction> actions;

    public Cell toCell() {
        if (isNull(actions)) {
            return null;
        }
        Cell list = CellBuilder.beginCell().endCell();

        for (OutAction action : actions) {
            Cell outAction = action.toCell();
            list = CellBuilder.beginCell()
                    .storeRef(list)
                    .storeCell(outAction)
                    .endCell();
        }
        return list;
    }

    public static OutList deserialize(CellSlice cs) {
        List<OutAction> actions = new ArrayList<>();
        while (cs.getRefsCount() != 0) {
            Cell t = cs.loadRef();
            actions.add(OutAction.deserialize(CellSlice.beginParse(cs)));
            cs = CellSlice.beginParse(t);
        }
        return OutList.builder()
                .actions(actions)
                .build();
    }
}
