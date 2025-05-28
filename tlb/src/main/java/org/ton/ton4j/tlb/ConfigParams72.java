package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * oracle_bridge_params#_
 * bridge_address:bits256
 * oracle_mutlisig_address:bits256
 * oracles:(HashmapE 256 uint256)
 * external_chain_address:bits256 = OracleBridgeParams;
 * _ OracleBridgeParams = ConfigParam 72; // Binance Smart Chain bridge
 * </pre>
 */
@Builder
@Data
public class ConfigParams72 implements Serializable {
  OracleBridgeParams binanceSmartChainBridge;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(binanceSmartChainBridge.toCell()).endCell();
  }

  public static ConfigParams72 deserialize(CellSlice cs) {
    return ConfigParams72.builder()
        .binanceSmartChainBridge(OracleBridgeParams.deserialize(cs))
        .build();
  }
}
