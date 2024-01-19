package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
public class TickTock {
    boolean tick;
    boolean tock;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeBit(tick)
                .storeBit(tock)
                .endCell();
    }
}
