package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapE;

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
    BigInteger depth;  // `tlb:"maybe ## 5"`
    TickTock tickTock; // `tlb:"maybe ."`
    Cell code; // `tlb:"maybe ^"`
    Cell data; // `tlb:"maybe ^"`
    TonHashMapE lib; // `tlb:"dictE 256"`

    public Cell toCell() {
        return CellBuilder.beginCell() // todo review
                .storeBit(nonNull(depth))
                .storeBit(nonNull(tickTock))
                .storeBit(nonNull(code))
                .storeBit(nonNull(data))
                .storeBit(nonNull(lib))
                .storeRef(code)
                .storeRef(data)
                .storeRef(nonNull(lib) ? lib.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).bits,
                        v -> CellBuilder.beginCell().storeUint((byte) v, 256)
                ) : CellBuilder.beginCell().storeBit(false).endCell()).endCell();
    }
}
