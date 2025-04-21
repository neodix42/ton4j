package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

/** _ fundamental_smc_addr:(HashmapE 256 True) = ConfigParam 31; */
@Builder
@Data
public class ConfigParams31 implements Serializable {
  TonHashMapE fundamentalSmcAddr;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeDict(
            fundamentalSmcAddr.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().endCell()))
        .endCell();
  }

  public static ConfigParams31 deserialize(CellSlice cs) {
    return ConfigParams31.builder()
        .fundamentalSmcAddr((cs.loadDictE(256, k -> k.readInt(256), v -> v)))
        .build();
  }
}
