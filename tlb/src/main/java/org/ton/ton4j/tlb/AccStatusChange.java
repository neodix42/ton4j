package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

@Builder
@Data
public class AccStatusChange implements Serializable {
  String type;

  public Cell toCell() {
    switch (type) {
      case "UNCHANGED":
        {
          return CellBuilder.beginCell().storeUint(0b0, 1).endCell();
        }
      case "FROZEN":
        {
          return CellBuilder.beginCell().storeUint(0b01, 2).endCell();
        }
      case "DELETED":
        {
          return CellBuilder.beginCell().storeUint(0b10, 2).endCell();
        }
    }
    throw new Error("unknown account status change");
  }

  public static AccStatusChange deserialize(CellSlice cs) {
    boolean isChanged = cs.loadBit();
    if (isChanged) {
      boolean isDeleted = cs.loadBit();
      if (isDeleted) {
        return AccStatusChange.builder().type("DELETED").build();
      }
      return AccStatusChange.builder().type("FROZEN").build();
    }
    return AccStatusChange.builder().type("UNCHANGED").build();
  }
}
