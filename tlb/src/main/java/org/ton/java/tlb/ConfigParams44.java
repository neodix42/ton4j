package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

@Builder
@Data
public class ConfigParams44 {
  int magic;
  TonHashMapE suspendedAddressList;
  long suspendedUntil;

  public Cell toCell() {

    return CellBuilder.beginCell()
        .storeUint(0x00, 8)
        .storeDict(
            suspendedAddressList.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 288).endCell().getBits(),
                v -> CellBuilder.beginCell().endCell()))
        .storeUint(suspendedUntil, 32)
        .endCell();
  }

  public static ConfigParams44 deserialize(CellSlice cs) {
    return ConfigParams44.builder()
        .suspendedAddressList(cs.loadDictE(288, k -> k.readUint(288), v -> v))
        .build();
  }
}
