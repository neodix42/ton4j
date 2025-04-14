package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** vmc_until$110000 body:^VmCont after:^VmCont = VmCont; */
@Builder
@Data
public class VmcWhileBody implements VmCont, Serializable {
  long magic;
  VmCont cond;
  VmCont body;
  VmCont after;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b110011, 6)
        .storeRef(cond.toCell())
        .storeRef(body.toCell())
        .storeRef(after.toCell())
        .endCell();
  }

  public static VmcWhileBody deserialize(CellSlice cs) {
    return VmcWhileBody.builder()
        .magic(cs.loadUint(6).intValue())
        .cond(VmCont.deserialize(CellSlice.beginParse(cs.loadRef())))
        .body(VmCont.deserialize(CellSlice.beginParse(cs.loadRef())))
        .after(VmCont.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
