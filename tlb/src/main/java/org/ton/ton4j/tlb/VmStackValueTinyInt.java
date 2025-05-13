package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** vm_stk_tinyint#01 value:int64 = VmStackValue; */
@Builder
@Data
public class VmStackValueTinyInt implements VmStackValue, Serializable {
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
