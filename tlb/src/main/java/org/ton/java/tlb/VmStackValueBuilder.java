package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** vm_stk_builder#05 cell:^Cell = VmStackValue; */
@Builder
@Data
public class VmStackValueBuilder implements VmStackValue, Serializable {
  int magic;
  Cell cell;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0x05, 8).storeRef(cell).endCell();
  }

  public static VmStackValueBuilder deserialize(CellSlice cs) {
    return VmStackValueBuilder.builder()
        .magic(cs.loadUint(8).intValue())
        .cell(cs.loadRef())
        .build();
  }
}
