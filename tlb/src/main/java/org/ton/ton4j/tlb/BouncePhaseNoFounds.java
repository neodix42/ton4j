package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * tr_phase_bounce_nofunds$01
 *   msg_size:StorageUsed
 *   req_fwd_fees:Grams = TrBouncePhase;
 *   </pre>
 */
@Builder
@Data
public class BouncePhaseNoFounds implements BouncePhase, Serializable {
  int magic;
  StorageUsed msgSize;
  BigInteger reqFwdFees;

  private String getMagic() {
    return Integer.toHexString(magic);
  }

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(1, 2)
        .storeSlice(CellSlice.beginParse(msgSize.toCell()))
        .storeCoins(reqFwdFees)
        .endCell();
  }

  public static BouncePhaseNoFounds deserialize(CellSlice cs) {
    long magic = cs.loadUint(2).intValue();
    assert (magic == 0b01)
        : "BouncePhaseNoFounds: magic not equal to 0b01, found 0x" + Long.toHexString(magic);

    return BouncePhaseNoFounds.builder()
        .msgSize(StorageUsed.deserialize(cs))
        .reqFwdFees(cs.loadCoins())
        .build();
  }
}
