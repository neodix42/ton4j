package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * block_grams_created#6b
 * masterchain_block_fee:Grams
 * basechain_block_fee:Grams  = BlockCreateFees;
 * _ BlockCreateFees = ConfigParam 14;
 * </pre>
 */
@Builder
@Data
public class ConfigParams14 implements Serializable {
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
