package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Data
public class ConfigParams6 {
    BigInteger mintNewPrice;
    BigInteger mintAddPrice;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCoins(mintNewPrice)
                .storeCoins(mintAddPrice)
                .endCell();
    }

    public static ConfigParams6 deserialize(CellSlice cs) {
        return ConfigParams6.builder()
                .mintNewPrice(cs.loadCoins())
                .mintAddPrice(cs.loadCoins())
                .build();
    }
}
