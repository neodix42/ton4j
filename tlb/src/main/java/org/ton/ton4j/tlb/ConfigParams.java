package org.ton.ton4j.tlb;

import java.io.Serializable;
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
  Cell config;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(configAddr.toBigInteger(), 256)
        .storeCell(config)
        .endCell();
  }

  public static ConfigParams deserialize(CellSlice cs) {
    ConfigParams configParams =
        ConfigParams.builder().configAddr(Address.of(cs.loadBits(256).toByteArray())).build();
    configParams.setConfig(cs.loadRef());
    return configParams;
  }
}
