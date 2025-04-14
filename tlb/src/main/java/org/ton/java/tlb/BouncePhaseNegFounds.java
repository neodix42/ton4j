package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * tr_phase_bounce_negfunds$00 = TrBouncePhase;
 * </pre>
 */
@Builder
@Data
public class BouncePhaseNegFounds implements BouncePhase, Serializable {
  int magic;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0, 2).endCell();
  }

  public static BouncePhaseNegFounds deserialize(CellSlice cs) {
    long magic = cs.loadUint(1).intValue(); // review, should be 2
    assert (magic == 0b0)
        : "BouncePhaseNegFounds: magic not equal to 0b0, found 0x" + Long.toHexString(magic);

    return BouncePhaseNegFounds.builder().build();
  }
}
