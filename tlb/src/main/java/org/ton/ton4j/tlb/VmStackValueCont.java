package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** vm_stk_cont#06 cont:VmCont = VmStackValue; */
@Builder
@Data
public class VmStackValueCont implements VmStackValue, Serializable {
  int magic;
  VmCont cont;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0x06, 8).storeCell(cont.toCell()).endCell();
  }

  public static VmStackValueCont deserialize(CellSlice cs) {
    return VmStackValueCont.builder()
        .magic(cs.loadUint(8).intValue())
        .cont(VmCont.deserialize(cs))
        .build();
  }
}
