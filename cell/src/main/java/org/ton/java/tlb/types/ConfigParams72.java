package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
@ToString
public class ConfigParams72 {
    OracleBridgeParams binanceSmartChainBridge;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(binanceSmartChainBridge.toCell())
                .endCell();
    }

    public static ConfigParams72 deserialize(CellSlice cs) {
        return ConfigParams72.builder()
                .binanceSmartChainBridge(OracleBridgeParams.deserialize(cs))
                .build();
    }
}
