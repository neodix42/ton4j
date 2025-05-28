package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** vm_stk_slice#04 _:VmCellSlice = VmStackValue; */
@Builder
@Data
public class VmStackValueSlice implements VmStackValue, Serializable {
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
