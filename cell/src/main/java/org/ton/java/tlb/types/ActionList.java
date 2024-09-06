package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.util.List;

@Builder
@Getter
@Setter
@ToString
public class ActionList {
    List<ExtendedAction> actions;

    public Cell toCell() {
        Cell list = CellBuilder.beginCell().endCell();
        for (ExtendedAction action : actions) {
            Cell cell = action.toCell();
            list = CellBuilder.beginCell()
                    .storeRef(list)
                    .storeCell(cell)
                    .endCell();
        }
        return list;
    }

    // todo...
//    public static ActionList deserialize(CellSlice cs) {
//        List<ExtendedAction> actions = new ArrayList<>();
//        while (cs.getRefsCount() != 0) {
//            Cell t = cs.loadRef();
//            ExtendedAction action = ExtendedAction.deserialize(CellSlice.beginParse(cs));
//            actions.add(action);
//            cs = CellSlice.beginParse(t);
//        }
//        return ActionList.builder()
//                .actions(actions)
//                .build();
//    }
}
