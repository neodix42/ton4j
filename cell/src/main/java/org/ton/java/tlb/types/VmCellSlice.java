package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>{@code
 * _ cell:^Cell st_bits:(## 10) end_bits:(## 10) { st_bits <= end_bits } st_ref:(#<= 4) end_ref:(#<= 4) { st_ref <= end_ref } = VmCellSlice;
 * }</pre>
 */
@Builder
@Data
public class VmCellSlice {
  Cell cell;
  int stBits;
  int endBits;
  int stRef;
  int endRef;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeRef(cell)
        .storeUint(stBits, 10)
        .storeUint(endBits, 10)
        .storeUint(stRef, 3)
        .storeUint(endRef, 3)
        .endCell();
  }

  public static VmCellSlice deserialize(CellSlice cs) {
    return VmCellSlice.builder()
        .cell(cs.loadRef())
        .stBits(cs.loadUint(10).intValue())
        .endBits(cs.loadUint(10).intValue())
        .stRef(cs.loadUint(3).intValue())
        .endRef(cs.loadUint(3).intValue())
        .build();
  }
}
