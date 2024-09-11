package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * vm_stack#_ depth:(## 24) stack:(VmStackList depth) = VmStack;
 */
@Builder
@Data
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
        int depth = cs.loadUint(24).intValue();
        return VmStack.builder()
                .depth(depth)
                .stack(VmStackList.deserialize(cs, depth))
                .build();
    }
}
