package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * vmc_until$110000 body:^VmCont after:^VmCont = VmCont;
 */
@Builder
@Data
public class VmcAgain implements VmCont {
    long magic;
    VmCont body;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b110001, 6)
                .storeRef(body.toCell())
                .endCell();
    }

    public static VmcAgain deserialize(CellSlice cs) {
        return VmcAgain.builder()
                .magic(cs.loadUint(6).intValue())
                .body(VmCont.deserialize(CellSlice.beginParse(cs.loadRef())))
                .build();
    }
}
