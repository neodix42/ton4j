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
public class GasLimitsPricesExt implements GasLimitsPrices, Serializable {
  long magic;
  BigInteger gasPrice;
  BigInteger gasLimit;
  BigInteger specialGasLimit;
  BigInteger gasCredit;
  BigInteger blockGasLimit;
  BigInteger freezeDueLimit;
  BigInteger deleteDueLimit;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xde, 8)
        .storeUint(gasPrice, 64)
        .storeUint(gasLimit, 64)
        .storeUint(specialGasLimit, 64)
        .storeUint(gasCredit, 64)
        .storeUint(blockGasLimit, 64)
        .storeUint(freezeDueLimit, 64)
        .storeUint(deleteDueLimit, 64)
        .endCell();
  }

  public static GasLimitsPricesExt deserialize(CellSlice cs) {
    return GasLimitsPricesExt.builder()
        .magic(cs.loadUint(8).longValue())
        .gasPrice(cs.loadUint(64))
        .gasLimit(cs.loadUint(64))
        .specialGasLimit(cs.loadUint(64))
        .gasCredit(cs.loadUint(64))
        .blockGasLimit(cs.loadUint(64))
        .freezeDueLimit(cs.loadUint(64))
        .deleteDueLimit(cs.loadUint(64))
        .build();
  }
}
