package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * vm_tuple_tcons$_ {n:#} head:(VmTupleRef n) tail:^VmStackValue = VmTuple (n + 1);
 */
@Builder
@Getter
@Setter
@ToString
public class VmTuple {
    VmTupleRef head;
    VmStackValue tail;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(head.toCell())
                .storeRef(tail.toCell())
                .endCell();
    }

    public static VmTuple deserialize(CellSlice cs) {
        return VmTuple.builder()
                .head(VmTupleRef.deserialize(cs))
                .tail(cs.getRefsCount() > 0 ? VmStackValue.deserialize(CellSlice.beginParse(cs.loadRef())) : null)
                .build();
    }
}
