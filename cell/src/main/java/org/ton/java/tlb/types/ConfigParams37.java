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
public class ConfigParams37 {
    ValidatorSet nextTempValidatorSet;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(nextTempValidatorSet.toCell())
                .endCell();
    }

    public static ConfigParams37 deserialize(CellSlice cs) {
        return ConfigParams37.builder()
                .nextTempValidatorSet(ValidatorSet.deserialize(cs))
                .build();
    }
}
