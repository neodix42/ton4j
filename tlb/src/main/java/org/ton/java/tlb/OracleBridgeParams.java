package org.ton.java.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

@Builder
@Data
public class OracleBridgeParams {
  Address bridgeAddress;
  Address oracleMultiSigAddress;
  TonHashMapE oracles;
  BigInteger externalChainAddress;

  public Cell toCell() {

    return CellBuilder.beginCell()
        .storeAddress(bridgeAddress)
        .storeAddress(oracleMultiSigAddress)
        .storeDict(
            oracles.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeUint((BigInteger) v, 256).endCell()))
        .storeUint(externalChainAddress, 256)
        .endCell();
  }

  public static OracleBridgeParams deserialize(CellSlice cs) {
    return OracleBridgeParams.builder()
        .bridgeAddress(cs.loadAddress())
        .oracleMultiSigAddress(cs.loadAddress())
        .oracles(
            cs.loadDictE(256, k -> k.readUint(256), v -> CellSlice.beginParse(v).loadUint(256)))
        .build();
  }
}
