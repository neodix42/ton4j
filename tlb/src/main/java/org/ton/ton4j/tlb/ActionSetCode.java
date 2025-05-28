package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * action_set_code#ad4de08e new_code:^Cell = OutAction;
 * </pre>
 */
@Builder
@Data
public class ActionSetCode implements OutAction, Serializable {
  long magic;
  Cell newCode;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0xad4de08e, 32).storeRef(newCode).endCell();
  }

  public static ActionSetCode deserialize(CellSlice cs) {
    return ActionSetCode.builder()
        .magic(cs.loadUint(32).intValue())
        .newCode(cs.sliceToCell())
        .build();
  }
}
