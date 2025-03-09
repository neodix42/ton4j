package org.ton.java.emulator.tvm;

import java.io.Serializable;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.*;

@Builder
@Data
public class SendExternalMessageResult implements Serializable {
  boolean success;
  @ToString.Exclude String new_code; // Base64 boc decoded new code cell
  @ToString.Exclude String new_data; // Base64 boc decoded new data cell
  boolean accepted;
  int vm_exit_code;
  String vm_log;
  String missing_library;
  int gas_used;

  @ToString.Exclude
  String actions; // Base64 boc decoded compute phase actions cell of type (OutList n)

  @ToString.Include(name = "newCodeBase64")
  public String getNewCodeBase64() {
    return StringUtils.isNotEmpty(new_code) ? new_code : "";
  }

  @ToString.Include(name = "newDataBase64")
  public String getNewDataBase64() {
    return StringUtils.isNotEmpty(new_data) ? new_data : "";
  }

  @ToString.Include(name = "actionsBase64")
  public String getActionsBase64() {
    return StringUtils.isNotEmpty(actions) ? actions : "";
  }

  public OutList getActions() {
    return OutList.deserialize(CellSlice.beginParse(Cell.fromBocBase64(actions)));
  }

  public Cell getNewCodeCell() {
    return Cell.fromBocBase64(new_code);
  }

  public Cell getNewDataCell() {
    return Cell.fromBocBase64(new_data);
  }
}
