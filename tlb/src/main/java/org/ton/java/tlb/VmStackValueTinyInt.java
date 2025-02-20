package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/** vm_stk_tinyint#01 value:int64 = VmStackValue; */
@Builder
@Data
public class VmStackValueTinyInt implements VmStackValue {
  long magic;
  BigInteger value;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0x01, 8).storeInt(value, 64).endCell();
  }

  public static VmStackValueTinyInt deserialize(CellSlice cs) {
    return VmStackValueTinyInt.builder()
        .magic(cs.loadUint(8).intValue())
        .value(cs.loadInt(64))
        .build();
  }
}
