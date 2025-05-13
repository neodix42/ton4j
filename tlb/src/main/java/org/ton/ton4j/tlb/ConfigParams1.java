package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** _ elector_addr:bits256 = ConfigParam 1; */
@Builder
@Data
public class ConfigParams1 implements Serializable {
  BigInteger electorAddr;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(electorAddr, 256).endCell();
  }

  public static ConfigParams1 deserialize(CellSlice cs) {
    return ConfigParams1.builder().electorAddr(cs.loadUint(256)).build();
  }
}
