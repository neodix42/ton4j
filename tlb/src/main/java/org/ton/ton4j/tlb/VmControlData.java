package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 * vm_ctl_data$_ nargs:(Maybe uint13) stack:(Maybe VmStack) save:VmSaveList cp:(Maybe int16) =
 * VmControlData;
 */
@Builder
@Data
public class VmControlData implements Serializable {
  BigInteger nargs;
  VmStack stack;
  VmSaveList save;
  BigInteger cp;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUintMaybe(nargs, 13)
        .storeCellMaybe(stack.toCell())
        .storeCell(save.toCell())
        .storeIntMaybe(cp, 16)
        .endCell();
  }

  public static VmControlData deserialize(CellSlice cs) {
    return VmControlData.builder()
        .nargs(cs.loadUintMaybe(13))
        .stack(cs.loadBit() ? VmStack.deserialize(cs) : null)
        .save(VmSaveList.deserialize(cs))
        .cp(cs.loadIntMaybe(16))
        .build();
  }
}
