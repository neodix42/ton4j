package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

import static java.util.Objects.nonNull;

@Builder
@Getter
@Setter
@ToString
/**
 * _ split_depth:(Maybe (## 5))
 *   special:(Maybe TickTock)
 *   code:(Maybe ^Cell)
 *   data:(Maybe ^Cell)
 *   library:(Maybe ^Cell) = StateInit;
 */
public class StateInit {
    BigInteger depth;
    TickTock tickTock;
    Cell code;
    Cell data;
    Cell lib;

    public Cell toCell() {
        return CellBuilder.beginCell() // todo review
                .storeBit(nonNull(depth))
                .storeBit(nonNull(tickTock))
                .storeBit(nonNull(code))
                .storeBit(nonNull(data))
                .storeBit(nonNull(lib))
                .storeCellMaybe(tickTock.toCell())
                .storeRefMaybe(code)
                .storeRefMaybe(data)
                .storeRefMaybe(lib)
                .endCell();
    }
}
