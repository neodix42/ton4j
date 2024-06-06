package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * vm_stk_nan#02ff = VmStackValue;
 */
@Builder
@Getter
@Setter
@ToString
public class VmStackValueNaN implements VmStackValue {
    int value;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x02ff, 16)
                .endCell();
    }

    public static VmStackValueNaN deserialize(CellSlice cs) {
        return VmStackValueNaN.builder()
                .value(cs.loadUint(16).intValue())
                .build();
    }
}
