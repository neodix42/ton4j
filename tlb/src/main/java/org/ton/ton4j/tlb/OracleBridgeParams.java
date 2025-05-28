package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

@Builder
@Data
public class OracleBridgeParams implements Serializable {
  BigInteger bridgeAddress;
  BigInteger oracleMultiSigAddress;
  TonHashMapE oracles;
  BigInteger externalChainAddress;

  public Cell toCell() {

    return CellBuilder.beginCell()
        .storeUint(bridgeAddress, 256)
        .storeUint(oracleMultiSigAddress, 256)
        .storeDict(
            oracles.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeUint((BigInteger) v, 256).endCell()))
        .storeUint(externalChainAddress, 256)
        .endCell();
  }

  public static OracleBridgeParams deserialize(CellSlice cs) {
    return OracleBridgeParams.builder()
        .bridgeAddress(cs.loadUint(256))
        .oracleMultiSigAddress(cs.loadUint(256))
        .oracles(
            cs.loadDictE(256, k -> k.readUint(256), v -> CellSlice.beginParse(v).loadUint(256)))
        .build();
  }
}
