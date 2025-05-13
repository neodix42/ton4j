package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** vm_stack#_ depth:(## 24) stack:(VmStackList depth) = VmStack; */
@Builder
@Data
public class VmStack implements Serializable {
  int depth;
  VmStackList stack;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(depth, 24).storeCell(stack.toCell()).endCell();
  }

  public static VmStack deserialize(CellSlice cs) {
    int depth = cs.loadUint(24).intValue();
    return VmStack.builder().depth(depth).stack(VmStackList.deserialize(cs, depth)).build();
  }
}
