package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * vm_stk_cons#_ {n:#} rest:^(VmStackList n) tos:VmStackValue = VmStackList (n + 1);
 * vm_stk_nil#_ = VmStackList 0;
 */
@Builder
@Getter
@Setter
@ToString
public class VmStackList {
    Cell rest;
    VmStackValue value;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeRef(rest)
                .storeCell(value.toCell())
                .endCell();
    }

    public static VmStackList deserialize(CellSlice cs) {
        return VmStackList.builder()
                .value(VmStackValue.deserialize(cs))
                .build();
    }
}
