package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Data
public class MsgForwardPrices {
    int magic;
    BigInteger lumpPrice;
    BigInteger bitPrice;
    BigInteger cellPrice;
    long ihrPriceFactor;
    int firstFrac;
    int nextFrac;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0xea, 8)
                .storeUint(lumpPrice, 64)
                .storeUint(bitPrice, 64)
                .storeUint(cellPrice, 64)
                .storeUint(ihrPriceFactor, 32)
                .storeUint(firstFrac, 16)
                .storeUint(nextFrac, 16)
                .endCell();
    }

    public static MsgForwardPrices deserialize(CellSlice cs) {
        return MsgForwardPrices.builder()
                .magic(cs.loadUint(8).intValue())
                .lumpPrice(cs.loadUint(64))
                .bitPrice(cs.loadUint(64))
                .cellPrice(cs.loadUint(64))
                .ihrPriceFactor(cs.loadUint(32).longValue())
                .firstFrac(cs.loadUint(16).intValue())
                .nextFrac(cs.loadUint(16).intValue())
                .build();
    }
}
