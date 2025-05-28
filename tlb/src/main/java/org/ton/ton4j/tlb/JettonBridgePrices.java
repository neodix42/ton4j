package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
public class JettonBridgePrices implements Serializable {
  BigInteger bridgeBurnFee;
  BigInteger bridgeMintFee;
  BigInteger walletMinTonsForStorage;
  BigInteger walletGasConsumption;
  BigInteger minterMinTonsForStorage;
  BigInteger discoverGasConsumption;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeCoins(bridgeBurnFee)
        .storeCoins(bridgeMintFee)
        .storeCoins(walletMinTonsForStorage)
        .storeCoins(walletGasConsumption)
        .storeCoins(minterMinTonsForStorage)
        .storeCoins(discoverGasConsumption)
        .endCell();
  }

  public static JettonBridgePrices deserialize(CellSlice cs) {
    return JettonBridgePrices.builder()
        .bridgeBurnFee(cs.loadCoins())
        .bridgeMintFee(cs.loadCoins())
        .walletMinTonsForStorage(cs.loadCoins())
        .walletGasConsumption(cs.loadCoins())
        .minterMinTonsForStorage(cs.loadCoins())
        .discoverGasConsumption(cs.loadCoins())
        .build();
  }
}
