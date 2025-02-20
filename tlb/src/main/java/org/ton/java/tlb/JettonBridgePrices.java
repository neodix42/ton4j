package org.ton.java.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class JettonBridgePrices {
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
