package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** vm_stk_null#00 = VmStackValue; */
@Builder
@Data
public class VmStackValueNull implements VmStackValue {
  int value;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0, 8).endCell();
  }

  public static VmStackValueNull deserialize(CellSlice cs) {
    return VmStackValueNull.builder().value(cs.loadUint(8).intValue()).build();
  }
}
