package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams81 {
    JettonBridgeParams bnbTonTokenBridge;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(bnbTonTokenBridge.toCell())
                .endCell();
    }

    public static ConfigParams81 deserialize(CellSlice cs) {
        return ConfigParams81.builder()
                .bnbTonTokenBridge(JettonBridgeParams.deserialize(cs))
                .build();
    }
}
