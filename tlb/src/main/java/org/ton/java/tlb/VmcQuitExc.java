package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** vmc_quit_exc$1001 = VmCont; */
@Builder
@Data
public class VmcQuitExc implements VmCont, Serializable {
  long magic;
  long exitCode;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0b1001, 4).endCell();
  }

  public static VmcQuitExc deserialize(CellSlice cs) {
    return VmcQuitExc.builder().magic(cs.loadUint(4).intValue()).build();
  }
}
