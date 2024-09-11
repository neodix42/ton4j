package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams16 {
    long maxValidators;
    long maxMainValidators;
    long minValidators;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(maxValidators, 16)
                .storeUint(maxMainValidators, 16)
                .storeUint(minValidators, 16)
                .endCell();
    }

    public static ConfigParams16 deserialize(CellSlice cs) {
        return ConfigParams16.builder()
                .maxValidators(cs.loadUint(16).longValue())
                .maxMainValidators(cs.loadUint(16).longValue())
                .minValidators(cs.loadUint(16).longValue())
                .build();
    }
}
