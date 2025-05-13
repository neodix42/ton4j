package org.ton.ton4j.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

@Builder
@Data
public class JettonBridgeParamsV2 implements JettonBridgeParams {
  int magic;
  BigInteger bridgeAddress;
  BigInteger oracleAddress;
  TonHashMapE oracles;
  int stateFlags;
  JettonBridgePrices prices;
  BigInteger externalChainAddress;

  public Cell toCell() {

    return CellBuilder.beginCell()
        .storeUint(0x00, 8)
        .storeUint(bridgeAddress, 256)
        .storeUint(oracleAddress, 256)
        .storeDict(
            oracles.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeUint((BigInteger) v, 256).endCell()))
        .storeUint(stateFlags, 8)
        .storeRef(prices.toCell())
        .storeUint(externalChainAddress, 256)
        .endCell();
  }

  public static JettonBridgeParamsV2 deserialize(CellSlice cs) {
    return JettonBridgeParamsV2.builder()
        .magic(cs.loadUint(8).intValue())
        .bridgeAddress(cs.loadUint(256))
        .oracleAddress(cs.loadUint(256))
        .oracles(
            cs.loadDictE(256, k -> k.readUint(256), v -> CellSlice.beginParse(v).loadUint(256)))
        .stateFlags(cs.loadUint(8).intValue())
        .prices(JettonBridgePrices.deserialize(CellSlice.beginParse(cs.loadRef())))
        .externalChainAddress(cs.loadUint(256))
        .build();
  }
}
