package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** vmc_quit$1000 exit_code:int32 = VmCont; */
@Builder
@Data
public class VmcQuit implements VmCont, Serializable {
  long magic;
  long exitCode;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0b1000, 4).storeInt(exitCode, 32).endCell();
  }

  public static VmcQuit deserialize(CellSlice cs) {
    return VmcQuit.builder()
        .magic(cs.loadUint(4).intValue())
        .exitCode(cs.loadInt(32).longValue())
        .build();
  }
}
