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
public class ConfigParams22 {
    BlockLimits configMcBlockLimits;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(configMcBlockLimits.toCell())
                .endCell();
    }

    public static ConfigParams22 deserialize(CellSlice cs) {
        return ConfigParams22.builder()
                .configMcBlockLimits(BlockLimits.deserialize(cs))
                .build();
    }
}
