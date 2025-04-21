package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

@Builder
@Data
public class JettonBridgeParamsV1 implements JettonBridgeParams, Serializable {
  int magic;
  BigInteger bridgeAddress;
  BigInteger oracleAddress;
  TonHashMapE oracles;
  int stateFlags;
  BigInteger burnBridgeFee;

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
        .storeUint(burnBridgeFee, 256)
        .endCell();
  }

  public static JettonBridgeParamsV1 deserialize(CellSlice cs) {
    return JettonBridgeParamsV1.builder()
        .magic(cs.loadUint(8).intValue())
        .bridgeAddress(cs.loadUint(256))
        .oracleAddress(cs.loadUint(256))
        .oracles(
            cs.loadDictE(256, k -> k.readUint(256), v -> CellSlice.beginParse(v).loadUint(256)))
        .stateFlags(cs.loadUint(8).intValue())
        .burnBridgeFee(cs.loadCoins())
        .build();
  }
}
