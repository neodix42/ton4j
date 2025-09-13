package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.*;

/**
 *
 *
 * <pre>
 * config_addr:bits256 config:^(Hashmap 32 ^Cell) = ConfigParams;
 *   </pre>
 */
@Builder
@Data
public class ConfigParams implements Serializable {
  Address configAddr;
  TonHashMap config;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(configAddr.toBigInteger(), 256)
        .storeCell(
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
