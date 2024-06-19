package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * vm_stk_tuple#07 len:(## 16) data:(VmTuple len) = VmStackValue;
 */
@Builder
@Getter
@Setter
@ToString
public class VmStackValueTuple implements VmStackValue {
    int magic;
    int len;
    VmTuple data;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x07, 8)
                .storeUint(data.getValues().size(), 16)
                .storeCell(data.toCell())
                .endCell();
    }

    public static VmStackValueTuple deserialize(CellSlice cs) {
        int magic = cs.loadUint(8).intValue();
        int len = cs.loadUint(16).intValue();
        return VmStackValueTuple.builder()
                .magic(magic)
                .len(len)
                .data(VmTuple.deserialize(cs, len))
                .build();
    }
}
