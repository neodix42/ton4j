package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** vm_stk_nan#02ff = VmStackValue; */
@Builder
@Data
public class VmStackValueNaN implements VmStackValue {
  int magic;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0x02ff, 16).endCell();
  }

  public static VmStackValueNaN deserialize(CellSlice cs) {
    return VmStackValueNaN.builder().magic(cs.loadUint(16).intValue()).build();
  }
}
