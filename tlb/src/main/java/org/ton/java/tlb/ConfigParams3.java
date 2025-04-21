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
 * _ fee_collector_addr:bits256 = ConfigParam 3; // ConfigParam 1 is used if absent
 * </pre>
 */
@Builder
@Data
public class ConfigParams3 implements Serializable {
  BigInteger feeCollectorAddr;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(feeCollectorAddr, 256).endCell();
  }

  public static ConfigParams3 deserialize(CellSlice cs) {
    return ConfigParams3.builder().feeCollectorAddr(cs.loadUint(256)).build();
  }
}
