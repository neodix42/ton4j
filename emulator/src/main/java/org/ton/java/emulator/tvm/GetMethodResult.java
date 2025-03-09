package org.ton.java.emulator.tvm;

import java.io.Serializable;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.*;

@Builder
@Data
public class GetMethodResult implements Serializable {
  boolean success;
  String error;
  String vm_log;
  int vm_exit_code;
  @ToString.Exclude String stack; // Base64 encoded BoC serialized stack (VmStack)
  String missing_library;
  int gas_used;

  @ToString.Include(name = "stackBase64")
  public String getStackBase64() {
    return StringUtils.isNotEmpty(stack) ? stack : "";
  }

  public VmStack getStack() {
    if (StringUtils.isNotEmpty(stack)) {
      return VmStack.deserialize(CellSlice.beginParse(Cell.fromBocBase64(stack)));
    }
    return VmStack.builder().build();
  }

  public Cell getStackCell() {
    if (StringUtils.isNotEmpty(stack)) {
      return VmStack.deserialize(CellSlice.beginParse(Cell.fromBocBase64(stack))).toCell();
    }
    return VmStack.builder().build().toCell();
  }

  public Cell getStackFirstEntrySlice() {
    if (StringUtils.isNotEmpty(stack)) {
      return VmCellSlice.deserialize(
              CellSlice.beginParse(
                  VmStack.deserialize(CellSlice.beginParse(Cell.fromBocBase64(stack)))
                      .getStack()
                      .getTos()
                      .get(0)
                      .toCell()))
          .getCell();
    }
    return VmCellSlice.builder().build().toCell();
  }
}
