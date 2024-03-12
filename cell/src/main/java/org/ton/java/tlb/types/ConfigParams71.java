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
public class ConfigParams71 {
    OracleBridgeParams ethereumBridge;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(ethereumBridge.toCell())
                .endCell();
    }

    public static ConfigParams71 deserialize(CellSlice cs) {
        return ConfigParams71.builder()
                .ethereumBridge(OracleBridgeParams.deserialize(cs))
                .build();
    }
}
