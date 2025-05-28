package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMap;

/** _ mandatory_params:(Hashmap 32 True) = ConfigParam 9; */
@Builder
@Data
public class ConfigParams9 implements Serializable {
  TonHashMap mandatoryParams;

  public Cell toCell() {

    Cell dict;

    dict =
        mandatoryParams.serialize(
            k -> CellBuilder.beginCell().storeUint((BigInteger) k, 32).endCell().getBits(),
            v -> CellBuilder.beginCell().endCell());
    return CellBuilder.beginCell().storeDict(dict).endCell();
  }

  public static ConfigParams9 deserialize(CellSlice cs) {
    return ConfigParams9.builder()
        .mandatoryParams(cs.loadDict(32, k -> k.readUint(32), v -> v))
        .build();
  }
}
