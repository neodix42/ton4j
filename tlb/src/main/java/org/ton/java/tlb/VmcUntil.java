package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** vmc_until$110000 body:^VmCont after:^VmCont = VmCont; */
@Builder
@Data
public class VmcUntil implements VmCont {
  long magic;
  VmCont body;
  VmCont after;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b110000, 6)
        .storeRef(body.toCell())
        .storeRef(after.toCell())
        .endCell();
  }

  public static VmcUntil deserialize(CellSlice cs) {
    return VmcUntil.builder()
        .magic(cs.loadUint(6).intValue())
        .body(VmCont.deserialize(CellSlice.beginParse(cs.loadRef())))
        .after(VmCont.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
