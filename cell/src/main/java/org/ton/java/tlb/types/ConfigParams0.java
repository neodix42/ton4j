package org.ton.java.tlb.types;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
/** _ config_addr:bits256 = ConfigParam 0; */
public class ConfigParams0 {
  BigInteger configAddr;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(configAddr, 256).endCell();
  }

  public static ConfigParams0 deserialize(CellSlice cs) {
    return ConfigParams0.builder().configAddr(cs.loadUint(256)).build();
  }
}
