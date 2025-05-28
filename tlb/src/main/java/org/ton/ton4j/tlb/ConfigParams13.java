package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;
import java.math.BigInteger;

/**
 *
 *
 * <pre>
 * complaint_prices#1a
 * deposit:Grams
 * bit_price:Grams
 * cell_price:Grams = ComplaintPricing;
 * _ ComplaintPricing = ConfigParam 13;
 * </pre>
 */
@Builder
@Data
public class ConfigParams13 implements Serializable {
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
