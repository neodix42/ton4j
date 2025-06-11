package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

/** _ elector_addr:bits256 = ConfigParam 1; */
@Builder
@Data
public class ConfigParams1 implements Serializable {
  public BigInteger electorAddr;

  public String getElectorAddr() {
    if (electorAddr == null) {
      return "";
    }
    return Utils.bytesToHex(Utils.to32ByteArray(electorAddr));
  }

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(electorAddr, 256).endCell();
  }

  public static ConfigParams1 deserialize(CellSlice cs) {
    return ConfigParams1.builder().electorAddr(cs.loadUint(256)).build();
  }
}
