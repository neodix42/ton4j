package org.ton.java.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

@Builder
@Data
public class ConfigParams31 {
  TonHashMapE fundamentalSmcAddr;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeDict(
            fundamentalSmcAddr.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeBit((Boolean) v).endCell()))
        .endCell();
  }

  public static ConfigParams31 deserialize(CellSlice cs) {
    return ConfigParams31.builder()
        .fundamentalSmcAddr(
            (cs.loadDictE(256, k -> k.readInt(256), v -> CellSlice.beginParse(v).loadBit())))
        .build();
  }
}
