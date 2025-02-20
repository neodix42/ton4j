package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ParamLimits {
  int magic;
  long underload;
  long softLimit;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xc3, 8)
        .storeUint(underload, 32) // check? todo
        .storeUint(softLimit, 32)
        .endCell();
  }

  public static ParamLimits deserialize(CellSlice cs) {
    return ParamLimits.builder()
        .magic(cs.loadUint(8).intValue())
        .underload(cs.loadUint(32).intValue())
        .softLimit(cs.loadUint(32).intValue())
        .build();
  }
}
