package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** vm_stk_slice#04 _:VmCellSlice = VmStackValue; */
@Builder
@Data
public class VmStackValueSlice implements VmStackValue {
  int magic;
  VmCellSlice cell;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0x04, 8).storeCell(cell.toCell()).endCell();
  }

  public static VmStackValueSlice deserialize(CellSlice cs) {
    return VmStackValueSlice.builder()
        .magic(cs.loadUint(8).intValue())
        .cell(VmCellSlice.deserialize(cs))
        .build();
  }
}
