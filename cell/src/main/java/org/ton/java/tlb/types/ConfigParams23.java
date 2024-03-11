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
public class ConfigParams23 {
    BlockLimits configBlockLimits;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(configBlockLimits.toCell())
                .endCell();
    }

    public static ConfigParams23 deserialize(CellSlice cs) {
        return ConfigParams23.builder()
                .configBlockLimits(BlockLimits.deserialize(cs))
                .build();
    }
}
