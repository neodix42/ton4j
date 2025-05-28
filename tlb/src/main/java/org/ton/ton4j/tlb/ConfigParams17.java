package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * min_stake:Grams
 * max_stake:Grams
 * min_total_stake:Grams
 * max_stake_factor:uint32 = ConfigParam 17;
 * </pre>
 */
@Builder
@Data
public class ConfigParams17 implements Serializable {
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
