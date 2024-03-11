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
public class ConfigParams19 {
    long globalId;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(globalId, 32)
                .endCell();
    }

    public static ConfigParams19 deserialize(CellSlice cs) {
        return ConfigParams19.builder()
                .globalId(cs.loadUint(32).longValue())
                .build();
    }
}
