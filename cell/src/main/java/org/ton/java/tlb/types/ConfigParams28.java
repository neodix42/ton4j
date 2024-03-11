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
public class ConfigParams28 {
    CatchainConfig catchainConfig;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(catchainConfig.toCell())
                .endCell();
    }

    public static ConfigParams28 deserialize(CellSlice cs) {
        return ConfigParams28.builder()
                .catchainConfig(CatchainConfig.deserialize(cs))
                .build();
    }
}
