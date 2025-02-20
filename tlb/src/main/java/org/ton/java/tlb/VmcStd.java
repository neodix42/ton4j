package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** vmc_std$00 cdata:VmControlData code:VmCellSlice = VmCont; */
@Builder
@Data
public class VmcStd implements VmCont {
  long magic;
  VmControlData cdata;
  VmCellSlice code;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b00, 2)
        .storeCell(cdata.toCell())
        .storeCell(code.toCell())
        .endCell();
  }

  public static VmcStd deserialize(CellSlice cs) {
    return VmcStd.builder()
        .magic(cs.loadUint(2).intValue())
        .cdata(VmControlData.deserialize(cs))
        .code(VmCellSlice.deserialize(cs))
        .build();
  }
}
