package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams79 {
    JettonBridgeParams ethTonTokenBridge;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(ethTonTokenBridge.toCell())
                .endCell();
    }

    public static ConfigParams79 deserialize(CellSlice cs) {
        return ConfigParams79.builder()
                .ethTonTokenBridge(JettonBridgeParams.deserialize(cs))
                .build();
    }
}
