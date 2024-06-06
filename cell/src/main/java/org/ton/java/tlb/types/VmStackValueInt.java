package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 * vm_stk_int#0201_ value:int257 = VmStackValue;
 */
@Builder
@Getter
@Setter
@ToString
public class VmStackValueInt implements VmStackValue {
    long magic;
    BigInteger value;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x0201, 16)
                .storeUint(value, 256)
                .endCell();
    }

    public static VmStackValueInt deserialize(CellSlice cs) {
        return VmStackValueInt.builder()
                .magic(cs.loadUint(16).intValue())
                .value(cs.loadUint(256))
                .build();
    }
}
