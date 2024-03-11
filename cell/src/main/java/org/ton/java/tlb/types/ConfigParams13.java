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
public class ConfigParams13 {
    long magic;
    BigInteger deposit;
    BigInteger bitPrice;
    BigInteger cellPrice;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x1a, 8)
                .storeCoins(deposit)
                .storeCoins(bitPrice)
                .storeCoins(cellPrice)
                .endCell();
    }

    public static ConfigParams13 deserialize(CellSlice cs) {
        return ConfigParams13.builder()
                .magic(cs.loadUint(8).longValue())
                .deposit(cs.loadCoins())
                .bitPrice(cs.loadCoins())
                .cellPrice(cs.loadCoins())
                .build();
    }
}
