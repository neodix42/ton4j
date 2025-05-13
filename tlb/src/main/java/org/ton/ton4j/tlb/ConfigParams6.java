package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** _ mint_new_price:Grams mint_add_price:Grams = ConfigParam 6; */
@Builder
@Data
public class ConfigParams6 implements Serializable {
  BigInteger mintNewPrice;
  BigInteger mintAddPrice;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCoins(mintNewPrice).storeCoins(mintAddPrice).endCell();
  }

  public static ConfigParams6 deserialize(CellSlice cs) {
    return ConfigParams6.builder()
        .mintNewPrice(cs.loadCoins())
        .mintAddPrice(cs.loadCoins())
        .build();
  }
}
