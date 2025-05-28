package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** vmc_envelope$01 cdata:VmControlData next:^VmCont = VmCont; */
@Builder
@Data
public class VmcEnvelope implements VmCont, Serializable {
  long magic;
  VmControlData cdata;
  Cell next;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b01, 2)
        .storeCell(cdata.toCell())
        .storeRef(next)
        .endCell();
  }

  public static VmcEnvelope deserialize(CellSlice cs) {
    return VmcEnvelope.builder().magic(cs.loadUint(2).intValue()).next(cs.loadRef()).build();
  }
}
