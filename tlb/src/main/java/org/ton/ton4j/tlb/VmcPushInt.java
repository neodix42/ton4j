package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** vmc_envelope$01 cdata:VmControlData next:^VmCont = VmCont; */
@Builder
@Data
public class VmcPushInt implements VmCont, Serializable {
  long magic;
  BigInteger value;
  VmCont next;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b1111, 4)
        .storeInt(value, 32)
        .storeRef(next.toCell())
        .endCell();
  }

  public static VmcPushInt deserialize(CellSlice cs) {
    return VmcPushInt.builder()
        .magic(cs.loadUint(2).intValue())
        .next(VmCont.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
