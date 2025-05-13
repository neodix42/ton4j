package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** vmc_until$110000 body:^VmCont after:^VmCont = VmCont; */
@Builder
@Data
public class VmcWhileCond implements VmCont, Serializable {
  long magic;
  VmCont cond;
  VmCont body;
  VmCont after;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b110010, 6)
        .storeRef(cond.toCell())
        .storeRef(body.toCell())
        .storeRef(after.toCell())
        .endCell();
  }

  public static VmcWhileCond deserialize(CellSlice cs) {
    return VmcWhileCond.builder()
        .magic(cs.loadUint(6).intValue())
        .cond(VmCont.deserialize(CellSlice.beginParse(cs.loadRef())))
        .body(VmCont.deserialize(CellSlice.beginParse(cs.loadRef())))
        .after(VmCont.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
