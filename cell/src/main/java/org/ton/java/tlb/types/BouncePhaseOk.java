package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 * <pre>
 * tr_phase_bounce_ok$1
 *   msg_size:StorageUsedShort
 *   msg_fees:Grams
 *   fwd_fees:Grams = TrBouncePhase;
 *   </pre>
 */
@Builder
@Data

public class BouncePhaseOk implements BouncePhase {
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

    public static BouncePhaseOk deserialize(CellSlice cs) {
        long magic = cs.loadUint(1).intValue();
        assert (magic == 0b1) : "BouncePhaseok: magic not equal to 0b1, found 0x" + Long.toHexString(magic);

        return BouncePhaseOk.builder()
                .magic(0b1)
                .msgSize(StorageUsedShort.deserialize(cs))
                .msgFees(cs.loadCoins())
                .fwdFees(cs.loadCoins())
                .build();

    }
}
