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
public class VmcAgain implements VmCont, Serializable {
  long magic;
  VmCont body;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0b110001, 6).storeRef(body.toCell()).endCell();
  }

  public static VmcAgain deserialize(CellSlice cs) {
    return VmcAgain.builder()
        .magic(cs.loadUint(6).intValue())
        .body(VmCont.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
