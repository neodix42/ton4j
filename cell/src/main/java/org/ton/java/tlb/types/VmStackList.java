package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * Also knows as serializeTuple() in ton-core web.
     *
     * @return Cell
     */

    public Cell toCell() {
        Cell list = CellBuilder.beginCell().endCell();
        for (VmStackValue value : tos) {
            Cell valueCell = value.toCell();
            list = CellBuilder.beginCell()
                    .storeRef(list)
                    .storeCell(valueCell)
                    .endCell();
        }
        return list;
    }

    /**
     * Also known as parseTuple() in ton-core web.
     *
     * @param cs CellSlice
     * @return VmStackList
     */
    public static VmStackList deserialize(CellSlice cs, int depth) {

        if (depth == 0) {
            return VmStackList.builder().tos(Collections.emptyList()).build();
        }

        List<VmStackValue> ar1 = new ArrayList<>(deserialize(CellSlice.beginParse(cs.loadRef()), depth - 1).getTos());
        ar1.add(VmStackValue.deserialize(cs));
        return VmStackList.builder()
                .tos(ar1)
                .build();
    }
}
