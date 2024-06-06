package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.util.ArrayList;
import java.util.List;

/**
 * vm_stk_nil#_ = VmStackList 0;
 * vm_stk_cons#_ {n:#} rest:^(VmStackList n) tos:VmStackValue = VmStackList (n + 1);
 */
@Builder
@Getter
@Setter
@ToString
public class VmStackList {
    List<VmStackValue> tos;

    public Cell toCell() {
        Cell list = CellBuilder.beginCell().endCell();
        int i = 0;
        for (VmStackValue value : tos) {
            Cell valueCell = value.toCell();
            list = CellBuilder.beginCell()
                    .storeRef(list)
                    .storeCell(valueCell)
                    .endCell();
        }
        return list;
    }

    public static VmStackList deserialize(CellSlice cs) {
        List<VmStackValue> tos = new ArrayList<>();
        while (cs.getRefsCount() != 0) {
            Cell t = cs.loadRef();
            tos.add(VmStackValue.deserialize(CellSlice.beginParse(cs)));
            cs = CellSlice.beginParse(t);
        }
        return VmStackList.builder()
                .tos(tos)
                .build();
    }
}
