package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.io.Serializable;

/**
 *
 *
 * <pre>
 *     jetton_bridge_prices#_ bridge_burn_fee:Coins bridge_mint_fee:Coins
 *                        wallet_min_tons_for_storage:Coins
 *                        wallet_gas_consumption:Coins
 *                        minter_min_tons_for_storage:Coins
 *                        discover_gas_consumption:Coins = JettonBridgePrices;
 *
 * jetton_bridge_params_v0#00 bridge_address:bits256 oracles_address:bits256 oracles:(HashmapE 256 uint256) state_flags:uint8 burn_bridge_fee:Coins = JettonBridgeParams;
 * jetton_bridge_params_v1#01 bridge_address:bits256 oracles_address:bits256 oracles:(HashmapE 256 uint256) state_flags:uint8 prices:^JettonBridgePrices external_chain_address:bits256 = JettonBridgeParams;
 *
 * _ JettonBridgeParams = ConfigParam 79; // ETH->TON token bridge
 * </pre>
 */
@Builder
@Data
public class ConfigParams79 implements Serializable {
  JettonBridgeParams ethTonTokenBridge;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(ethTonTokenBridge.toCell()).endCell();
  }

  public static ConfigParams79 deserialize(CellSlice cs) {
    return ConfigParams79.builder().ethTonTokenBridge(JettonBridgeParams.deserialize(cs)).build();
  }
}
