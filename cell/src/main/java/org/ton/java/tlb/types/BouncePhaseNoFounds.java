package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * tr_phase_bounce_nofunds$01
 *   msg_size:StorageUsedShort
 *   req_fwd_fees:Grams = TrBouncePhase;
 */
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
}
