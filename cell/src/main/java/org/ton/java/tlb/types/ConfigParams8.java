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
public class ConfigParams8 {
    GlobalVersion globalVersion;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(globalVersion.toCell())
                .endCell();
    }

    public static ConfigParams8 deserialize(CellSlice cs) {
        return ConfigParams8.builder()
                .globalVersion(GlobalVersion.deserialize(cs))
                .build();
    }
}
