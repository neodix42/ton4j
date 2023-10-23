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
/**
 * anycast_info$_ depth:(#<= 30) { depth >= 1 } rewrite_pfx:(bits depth) = Anycast;
 */
public class Anycast {
    int depth; // 5 bits
    byte rewritePfx;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(depth, 5)
                .storeUint(rewritePfx, depth)
                .endCell();
    }
}
