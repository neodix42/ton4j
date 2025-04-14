package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.io.Serializable;

/** _ global_id:int32 = ConfigParam 19; */
@Builder
@Data
public class ConfigParams19 implements Serializable {
  long globalId;

  public Cell toCell() {
    return CellBuilder.beginCell().storeInt(globalId, 32).endCell();
  }

  public static ConfigParams19 deserialize(CellSlice cs) {
    return ConfigParams19.builder().globalId(cs.loadInt(32).longValue()).build();
  }
}
