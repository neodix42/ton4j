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
 * vm_tuple_nil$_ = VmTuple 0;
 * vm_tuple_tcons$_ {n:#} head:(VmTupleRef n) tail:^VmStackValue = VmTuple (n + 1);
 */
@Builder
@Getter
@Setter
@ToString
public class VmTuple {
    List<VmStackValue> values;

    public Cell toCell() {
        List<VmStackValue> pValues = new ArrayList<>(values);
        if (pValues.isEmpty()) {
            return CellBuilder.beginCell().endCell();
        }

        VmStackValue v = pValues.get(pValues.size() - 1);
        pValues.remove(pValues.size() - 1);
        return CellBuilder.beginCell()
                .storeCell(VmTupleRef.toCell(pValues)) // head
                .storeRef(v.toCell())                  // tail
                .endCell();
    }

    public static Cell toCell(List<VmStackValue> pValues) {
        List<VmStackValue> lValues = new ArrayList<>(pValues);

        if (lValues.isEmpty()) {
            return CellBuilder.beginCell().endCell();
        }

        VmStackValue v = lValues.get(lValues.size() - 1);
        lValues.remove(lValues.size() - 1);
        return CellBuilder.beginCell()
                .storeCell(VmTupleRef.toCell(lValues)) // head
                .storeRef(v.toCell())                  // tail
                .endCell();
    }

    public static VmTuple deserialize(CellSlice cs, int len) {
        if (len == 0) {
            return VmTuple.builder().build();
        }

        ArrayList<VmStackValue> ar = new ArrayList<>();
        ar.add((VmStackValue) VmTupleRef.deserialize(cs, len - 1).getValues().get(0));
        ar.add(cs.getRefsCount() > 0 ? VmStackValue.deserialize(CellSlice.beginParse(cs.loadRef())) : null);

        return VmTuple.builder()
                .values(ar)
                .build();
    }
}
