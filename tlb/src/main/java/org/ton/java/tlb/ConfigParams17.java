package org.ton.java.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams17 {
    BigInteger minStake;
    BigInteger maxStake;
    BigInteger minTotalStake;
    long maxStakeFactor;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCoins(minStake)
                .storeCoins(maxStake)
                .storeCoins(minTotalStake)
                .storeUint(maxStakeFactor, 16)
                .endCell();
    }

    public static ConfigParams17 deserialize(CellSlice cs) {
        return ConfigParams17.builder()
                .minStake(cs.loadCoins())
                .maxStake(cs.loadCoins())
                .minTotalStake(cs.loadCoins())
                .maxStakeFactor(cs.loadUint(32).longValue())
                .build();
    }
}
