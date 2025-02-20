package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 *
 *
 * <pre>
 * tr_phase_bounce_nofunds$01
 *   msg_size:StorageUsedShort
 *   req_fwd_fees:Grams = TrBouncePhase;
 *   </pre>
 */
@Builder
@Data
public class BouncePhaseNoFounds implements BouncePhase {
  int magic;
  StorageUsedShort msgSize;
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
        .msgSize(StorageUsedShort.deserialize(cs))
        .reqFwdFees(cs.loadCoins())
        .build();
  }
}
