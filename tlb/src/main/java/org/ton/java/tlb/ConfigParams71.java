package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * oracle_bridge_params#_
 * bridge_address:bits256
 * oracle_mutlisig_address:bits256
 * oracles:(HashmapE 256 uint256)
 * external_chain_address:bits256 = OracleBridgeParams;
 * _ OracleBridgeParams = ConfigParam 71; // Ethereum bridge
 * </pre>
 */
@Builder
@Data
public class ConfigParams71 implements Serializable {
  OracleBridgeParams ethereumBridge;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(ethereumBridge.toCell()).endCell();
  }

  public static ConfigParams71 deserialize(CellSlice cs) {
    return ConfigParams71.builder().ethereumBridge(OracleBridgeParams.deserialize(cs)).build();
  }
}
