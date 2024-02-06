package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
@ToString
/**
 * tr_phase_bounce_negfunds$00 = TrBouncePhase;
 */
public class BouncePhaseNegFounds implements BouncePhase {
    int magic;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0, 2)
                .endCell();
    }

    public static BouncePhaseNegFounds deserialize(CellSlice cs) {
        long magic = cs.loadUint(1).intValue(); // review, should be 2
        assert (magic == 0b0) : "BouncePhaseNegFounds: magic not equal to 0b0, found 0x" + Long.toHexString(magic);

        return BouncePhaseNegFounds.builder().build();
    }
}
