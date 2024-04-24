package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class JettonBridgeParamsV1 implements JettonBridgeParams {
    int magic;
    Address bridgeAddress;
    Address oracleAddress;
    TonHashMapE oracles;
    int stateFlags;
    BigInteger burnBridgeFee;

    public Cell toCell() {

        return CellBuilder.beginCell()
                .storeUint(0x00, 8)
                .storeAddress(bridgeAddress)
                .storeAddress(oracleAddress)
                .storeDict(oracles.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
                        v -> CellBuilder.beginCell().storeUint((BigInteger) v, 256).endCell()))
                .storeUint(stateFlags, 8)
                .storeUint(burnBridgeFee, 256)
                .endCell();
    }

    public static JettonBridgeParamsV1 deserialize(CellSlice cs) {
        return JettonBridgeParamsV1.builder()
                .magic(cs.loadUint(8).intValue())
                .bridgeAddress(cs.loadAddress())
                .oracleAddress(cs.loadAddress())
                .oracles(cs.loadDictE(256,
                        k -> k.readUint(256),
                        v -> CellSlice.beginParse(v).loadUint(256)))
                .stateFlags(cs.loadUint(8).intValue())
                .burnBridgeFee(cs.loadCoins())
                .build();
    }
}
