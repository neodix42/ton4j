package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * vm_stack#_ depth:(## 24) stack:(VmStackList depth) = VmStack;
 */
@Builder
@Getter
@Setter
@ToString
public class VmStack {
    int depth;
    VmStackList stack;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(depth, 24)
                .storeCell(stack.toCell())
                .endCell();
    }

    public static VmStack deserialize(CellSlice cs) {
        return VmStack.builder()
                .depth(cs.loadUint(24).intValue())
                .stack(VmStackList.deserialize(cs))
                .build();
    }
}
