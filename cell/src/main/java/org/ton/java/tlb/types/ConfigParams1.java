package org.ton.java.tlb.types;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams1 {
  BigInteger electorAddr;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(electorAddr, 256).endCell();
  }

  public static ConfigParams1 deserialize(CellSlice cs) {
    return ConfigParams1.builder().electorAddr(cs.loadUint(256)).build();
  }
}
