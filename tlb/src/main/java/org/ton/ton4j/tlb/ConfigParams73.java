package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

/**
 *
 *
 * <pre>
 * oracle_bridge_params#_
 * bridge_address:bits256
 * oracle_mutlisig_address:bits256
 * oracles:(HashmapE 256 uint256)
 * external_chain_address:bits256 = OracleBridgeParams;
 * _ OracleBridgeParams = ConfigParam 73; // Polygon bridge
 * </pre>
 */
@Builder
@Data
public class ConfigParams73 implements Serializable {
  OracleBridgeParams polygonBridge;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(polygonBridge.toCell()).endCell();
  }

  public static ConfigParams73 deserialize(CellSlice cs) {
    return ConfigParams73.builder().polygonBridge(OracleBridgeParams.deserialize(cs)).build();
  }
}
