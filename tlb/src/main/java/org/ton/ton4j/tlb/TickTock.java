package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
public class TickTock implements Serializable {
  boolean tick;
  boolean tock;

  public Cell toCell() {
    return CellBuilder.beginCell().storeBit(tick).storeBit(tock).endCell();
  }

  public static TickTock deserialize(CellSlice cs) {
    return TickTock.builder().tick(cs.loadBit()).tock(cs.loadBit()).build();
  }
}
