package org.ton.java.emulator.tvm;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.types.VmStack;

@Builder
@Setter
@Getter
@ToString
public class GetMethodResult implements Serializable {
  boolean success;
  String error;
  String vm_log;
  int vm_exit_code;
  String stack; // Base64 encoded BoC serialized stack (VmStack)
  String missing_library;
  int gas_used;

  public VmStack getStack() {
    if (StringUtils.isNotEmpty(stack)) {
      return VmStack.deserialize(CellSlice.beginParse(Cell.fromBocBase64(stack)));
    }
    return VmStack.builder().build();
  }
}
