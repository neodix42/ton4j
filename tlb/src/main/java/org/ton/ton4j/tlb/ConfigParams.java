package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMap;

@Builder
@Data
public class ConfigParams implements Serializable {
  Address configAddr;
  TonHashMap config;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeAddress(configAddr)
        .storeDict(
            config.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 32).endCell().getBits(),
                v -> CellBuilder.beginCell().storeRef((Cell) v).endCell()))
        .endCell();
  }

  public static ConfigParams deserialize(CellSlice cs) {
    return ConfigParams.builder()
        .configAddr(Address.of(cs.loadBits(256).toByteArray()))
        .config(
            CellSlice.beginParse(cs.loadRef())
                .loadDict(32, k -> k.readUint(32), v -> CellSlice.beginParse(v).loadRef()))
        .build();
  }
}
