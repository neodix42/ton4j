package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

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
