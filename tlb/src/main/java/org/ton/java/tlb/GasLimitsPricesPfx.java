package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Data
public class GasLimitsPricesPfx implements GasLimitsPrices {
  long magic;
  BigInteger flatGasLimit;
  BigInteger flatGasPrice;
  GasLimitsPrices other;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xd1, 8)
        .storeUint(flatGasLimit, 64)
        .storeUint(flatGasPrice, 64)
        .storeCell(other.toCell())
        .endCell();
  }

  public static GasLimitsPricesPfx deserialize(CellSlice cs) {
    return GasLimitsPricesPfx.builder()
        .magic(cs.loadUint(8).longValue())
        .flatGasLimit(cs.loadUint(64))
        .flatGasPrice(cs.loadUint(64))
        .other(GasLimitsPrices.deserialize(cs))
        .build();
  }
}
