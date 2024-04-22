package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * validators_ext#12
 *   utime_since:uint32
 *   utime_until:uint32
 *   total:(## 16)
 *   main:(## 16) { main <= total } { main >= 1 }
 *   total_weight:uint64
 *   list:(HashmapE 16 ValidatorDescr) = ValidatorSet;
 */
public class ValidatorsExt implements ValidatorSet {
    long magic;
    long uTimeSince;
    long uTimeUntil;
    int total;
    int main;
    BigInteger totalWeight;
    TonHashMapE list;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x12, 8)
                .storeUint(uTimeSince, 32)
                .storeUint(uTimeUntil, 32)
                .storeUint(total, 16)
                .storeUint(main, 16)
                .storeUint(totalWeight, 64)
                .storeDict(list.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 16).endCell().bits,
                        v -> CellBuilder.beginCell().storeCell(((ValidatorDescr) v).toCell()).endCell()
                ))
                .endCell();
    }

    public static Validators deserialize(CellSlice cs) {
        return Validators.builder()
                .magic(cs.loadUint(8).intValue())
                .uTimeSince(cs.loadUint(8).longValue())
                .uTimeUntil(cs.loadUint(8).longValue())
                .total(cs.loadUint(16).intValue())
                .main(cs.loadUint(16).intValue())
                .total(cs.loadUint(64).intValue())
                .list(cs.loadDictE(16,
                        k -> k.readInt(16),
                        v -> ValidatorDescr.deserialize(CellSlice.beginParse(v))))
                .build();
    }
}
