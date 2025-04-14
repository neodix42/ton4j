package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** vm_stk_int#0201_ value:int257 = VmStackValue; */
@Builder
@Data
public class VmStackValueInt implements VmStackValue, Serializable {
  long magic;
  BigInteger value;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0x0100, 15).storeInt(value, 257).endCell();
  }

  public static VmStackValueInt deserialize(CellSlice cs) {
    return VmStackValueInt.builder()
        .magic(cs.loadUint(15).intValue())
        .value(cs.loadInt(257))
        .build();
  }
}
