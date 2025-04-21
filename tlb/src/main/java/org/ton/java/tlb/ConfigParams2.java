package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** _ minter_addr:bits256 = ConfigParam 2; // ConfigParam 0 is used if absent */
@Builder
@Data
public class ConfigParams2 implements Serializable {
  BigInteger minterAddr;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(minterAddr, 256).endCell();
  }

  public static ConfigParams2 deserialize(CellSlice cs) {
    return ConfigParams2.builder().minterAddr(cs.loadUint(256)).build();
  }
}
