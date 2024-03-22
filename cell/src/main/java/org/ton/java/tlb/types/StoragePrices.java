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
public class StoragePrices {
    long magic;
    long utimeSince;
    BigInteger bitPricePs;
    BigInteger cellPricePs;
    BigInteger mcBitPricePs;
    BigInteger mcCellPricePs;


    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0xcc, 8)
                .storeUint(utimeSince, 32)
                .storeUint(bitPricePs, 64)
                .storeUint(cellPricePs, 64)
                .storeUint(mcBitPricePs, 64)
                .storeUint(mcCellPricePs, 64)
                .endCell();
    }

    public static StoragePrices deserialize(CellSlice cs) {
        return StoragePrices.builder()
                .magic(cs.loadUint(8).longValue())
                .utimeSince(cs.loadUint(32).longValue())
                .bitPricePs(cs.loadUint(64))
                .cellPricePs(cs.loadUint(64))
                .mcBitPricePs(cs.loadUint(64))
                .mcCellPricePs(cs.loadUint(64))
                .build();
    }
}
