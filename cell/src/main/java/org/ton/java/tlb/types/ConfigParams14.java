package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Data
public class ConfigParams14 {
    long magic;
    BigInteger masterchainBlockFee;
    BigInteger basechainBlockFee;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x6b, 8)
                .storeCoins(masterchainBlockFee)
                .storeCoins(basechainBlockFee)
                .endCell();
    }

    public static ConfigParams14 deserialize(CellSlice cs) {
        return ConfigParams14.builder()
                .magic(cs.loadUint(8).longValue())
                .masterchainBlockFee(cs.loadCoins())
                .basechainBlockFee(cs.loadCoins())
                .build();
    }
}
