package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * <pre>
 * anycast_info$_
 *  depth:(#<= 30) { depth >= 1 }
 *  rewrite_pfx:(bits depth)
 *  = Anycast;
 *  </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class Anycast {
    int depth; // 5 bits
    byte rewritePfx;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(depth, 5)
                .storeUint(rewritePfx, depth)
                .endCell();
    }

    public static Anycast deserialize(CellSlice cs) {
        int depth = cs.loadUint(5).intValue();
        return Anycast.builder()
                .depth(depth)
                .rewritePfx(cs.loadUint(depth).byteValueExact())
                .build();
    }
}
