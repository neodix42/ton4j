package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** vm_stk_cell#03 cell:^Cell = VmStackValue; */
@Builder
@Data
public class VmStackValueCell implements VmStackValue, Serializable {
  int magic;
  Cell cell;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0x03, 8).storeRef(cell).endCell();
  }

  public static VmStackValueCell deserialize(CellSlice cs) {
    return VmStackValueCell.builder().magic(cs.loadUint(8).intValue()).cell(cs.loadRef()).build();
  }
}
