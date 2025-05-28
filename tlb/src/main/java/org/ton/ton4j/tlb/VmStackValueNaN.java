package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** vm_stk_nan#02ff = VmStackValue; */
@Builder
@Data
public class VmStackValueNaN implements VmStackValue, Serializable {
  int magic;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0x02ff, 16).endCell();
  }

  public static VmStackValueNaN deserialize(CellSlice cs) {
    return VmStackValueNaN.builder().magic(cs.loadUint(16).intValue()).build();
  }
}
