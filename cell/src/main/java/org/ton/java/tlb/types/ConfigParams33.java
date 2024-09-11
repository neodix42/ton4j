package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams33 {
    ValidatorSet prevTempValidatorSet;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(prevTempValidatorSet.toCell())
                .endCell();
    }

    public static ConfigParams33 deserialize(CellSlice cs) {
        return ConfigParams33.builder()
                .prevTempValidatorSet(ValidatorSet.deserialize(cs))
                .build();
    }
}
