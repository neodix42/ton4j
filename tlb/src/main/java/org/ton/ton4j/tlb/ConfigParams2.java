package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

/** _ minter_addr:bits256 = ConfigParam 2; // ConfigParam 0 is used if absent */
@Builder
@Data
public class ConfigParams2 implements Serializable {
  public BigInteger minterAddr;

  public String getMinterAddr() {
    if (minterAddr == null) {
      return "";
    }
    return Utils.bytesToHex(Utils.to32ByteArray(minterAddr));
  }

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(minterAddr, 256).endCell();
  }

  public static ConfigParams2 deserialize(CellSlice cs) {
    return ConfigParams2.builder().minterAddr(cs.loadUint(256)).build();
  }
}
