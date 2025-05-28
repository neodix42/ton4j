package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

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
