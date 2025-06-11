package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

/** _ config_addr:bits256 = ConfigParam 0; */
@Builder
@Data
public class ConfigParams0 implements Serializable {
  public BigInteger configAddr;

  public String getConfigAddr() {
    if (configAddr == null) {
      return "";
    }
    return Utils.bytesToHex(Utils.to32ByteArray(configAddr));
  }

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(configAddr, 256).endCell();
  }

  public static ConfigParams0 deserialize(CellSlice cs) {
    return ConfigParams0.builder().configAddr(cs.loadUint(256)).build();
  }
}
