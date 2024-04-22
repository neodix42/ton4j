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
 action_set_code#ad4de08e new_code:^Cell = OutAction;
 */
public class ActionSetCode implements OutAction {
    long magic;
    Cell newCode;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0xad4de08e, 32)
                .storeRef(newCode)
                .endCell();
    }

    public static ActionSetCode deserialize(CellSlice cs) {
        return ActionSetCode.builder()
                .magic(cs.loadUint(32).intValue())
                .newCode(cs.sliceToCell())
                .build();
    }
}