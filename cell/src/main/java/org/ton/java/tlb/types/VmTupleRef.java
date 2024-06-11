package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * vm_tupref_nil$_ = VmTupleRef 0;
 * vm_tupref_single$_ entry:^VmStackValue = VmTupleRef 1;
 * vm_tupref_any$_ {n:#} ref:^(VmTuple (n + 2)) = VmTupleRef (n + 2);
 */
@Builder
@Getter
@Setter
@ToString
public class VmTupleRef {
    VmStackValue entry;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeRef(entry.toCell())
                .endCell();
    }

    public static VmTupleRef deserialize(CellSlice cs) { // more tests are required
        if (cs.getRefsCount() == 0) {
            return null;
        } else if (cs.getRefsCount() == 1) {
            return VmTupleRef.builder()
                    .entry(VmStackValue.deserialize(cs))
                    .build();
        } else {
            return VmTupleRef.builder()
                    .entry(VmStackValue.deserialize(CellSlice.beginParse(cs.loadRef())))
                    .build();
        }
    }
}
