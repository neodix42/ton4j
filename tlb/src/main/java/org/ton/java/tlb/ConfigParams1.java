package org.ton.java.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
/** _ elector_addr:bits256 = ConfigParam 1; */
public class ConfigParams1 {
  BigInteger electorAddr;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(electorAddr, 256).endCell();
  }

  public static ConfigParams1 deserialize(CellSlice cs) {
    return ConfigParams1.builder().electorAddr(cs.loadUint(256)).build();
  }
}
