package org.ton.ton4j.tlb;

import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * // Extended actions in W5:
 * action_list_basic$_ {n:#} actions:^(OutList n) = ActionList n 0;
 * action_list_extended$_ {m:#} {n:#} action:ExtendedAction prev:^(ActionList n m) = ActionList n (m+1);
 * </pre>
 */
@Builder
@Data
public class ActionList implements Serializable {
  List<ExtendedAction> actions;

  public Cell toCell() {

    Cell list = null;
    for (ExtendedAction action : actions) {
      Cell cell = action.toCell();
      list = CellBuilder.beginCell().storeCell(cell).storeRefMaybe(list).endCell();
    }
    return list;
  }

  public static ActionList deserialize(CellSlice cs) {
    List<ExtendedAction> actions = new ArrayList<>();
    actions.add(ExtendedAction.deserialize(CellSlice.beginParse(cs)));

    while (cs.getRefsCount() != 0) {
      Cell t = cs.loadMaybeRefX();
      if (nonNull(t)) {
        ExtendedAction action = ExtendedAction.deserialize(CellSlice.beginParse(t));
        actions.add(action);
        cs = CellSlice.beginParse(t);
      }
    }
    return ActionList.builder().actions(actions).build();
  }
}
