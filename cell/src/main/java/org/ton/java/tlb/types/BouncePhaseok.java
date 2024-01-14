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
 * tr_phase_bounce_ok$1
 *   msg_size:StorageUsedShort
 *   msg_fees:Grams
 *   fwd_fees:Grams = TrBouncePhase;
 */
public class BouncePhaseok implements BouncePhase {
    int magic;
    StorageUsedShort msgSize;
    BigInteger msgFees;
    BigInteger fwdFees;

    private String getMagic() {
        return Integer.toHexString(magic);
    }

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeBit(true)
                .storeSlice(CellSlice.beginParse(msgSize.toCell()))
                .storeCoins(msgFees)
                .storeCoins(fwdFees)
                .endCell();
    }
}
