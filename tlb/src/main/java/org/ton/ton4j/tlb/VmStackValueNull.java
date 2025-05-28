package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** vm_stk_null#00 = VmStackValue; */
@Builder
@Data
public class VmStackValueNull implements VmStackValue, Serializable {
  int value;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0, 8).endCell();
  }

  public static VmStackValueNull deserialize(CellSlice cs) {
    return VmStackValueNull.builder().value(cs.loadUint(8).intValue()).build();
  }
}
