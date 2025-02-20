package org.ton.java.emulator.tvm;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.*;

@Builder
@Setter
@Getter
@ToString
public class SendInternalMessageResult implements Serializable {
  boolean success;
  String new_code; // Base64 boc decoded new code cell
  String new_data; // Base64 boc decoded new data cell
  boolean accepted;
  int vm_exit_code;
  String vm_log;
  String missing_library;
  int gas_used;
  String actions; // Base64 boc decoded compute phase actions cell of type (OutList n)

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
