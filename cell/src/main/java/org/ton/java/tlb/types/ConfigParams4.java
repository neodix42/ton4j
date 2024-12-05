package org.ton.java.tlb.types;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams4 {
  BigInteger dnsRootAddr;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(dnsRootAddr, 256).endCell();
  }

  public static ConfigParams4 deserialize(CellSlice cs) {
    return ConfigParams4.builder().dnsRootAddr(cs.loadUint(256)).build();
  }
}
