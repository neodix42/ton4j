package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class GasLimitsPricesOrdinary implements GasLimitsPrices, Serializable {
  long magic;
  BigInteger gasPrice;
  BigInteger gasLimit;
  BigInteger gasCredit;
  BigInteger blockGasLimit;
  BigInteger freezeDueLimit;
  BigInteger deleteDueLimit;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xdd, 8)
        .storeUint(gasPrice, 64)
        .storeUint(gasLimit, 64)
        .storeUint(gasCredit, 64)
        .storeUint(blockGasLimit, 64)
        .storeUint(freezeDueLimit, 64)
        .storeUint(deleteDueLimit, 64)
        .endCell();
  }

  public static GasLimitsPricesOrdinary deserialize(CellSlice cs) {
    return GasLimitsPricesOrdinary.builder()
        .magic(cs.loadUint(8).longValue())
        .gasPrice(cs.loadUint(64))
        .gasLimit(cs.loadUint(64))
        .gasCredit(cs.loadUint(64))
        .blockGasLimit(cs.loadUint(64))
        .freezeDueLimit(cs.loadUint(64))
        .deleteDueLimit(cs.loadUint(64))
        .build();
  }
}
