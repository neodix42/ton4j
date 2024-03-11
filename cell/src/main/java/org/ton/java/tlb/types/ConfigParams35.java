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
public class ConfigParams35 {
    ValidatorSet currTempValidatorSet;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(currTempValidatorSet.toCell())
                .endCell();
    }

    public static ConfigParams35 deserialize(CellSlice cs) {
        return ConfigParams35.builder()
                .currTempValidatorSet(ValidatorSet.deserialize(cs))
                .build();
    }
}
