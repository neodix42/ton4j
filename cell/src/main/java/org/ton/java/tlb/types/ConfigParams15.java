package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams15 {
    long validatorsElectedFor;
    long electionsStartBefore;
    long electionsEndBefore;
    long stakeHeldFor;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(validatorsElectedFor, 32)
                .storeUint(electionsStartBefore, 32)
                .storeUint(electionsEndBefore, 32)
                .storeUint(stakeHeldFor, 32)
                .endCell();
    }

    public static ConfigParams15 deserialize(CellSlice cs) {
        return ConfigParams15.builder()
                .validatorsElectedFor(cs.loadUint(32).longValue())
                .electionsStartBefore(cs.loadUint(32).longValue())
                .validatorsElectedFor(cs.loadUint(32).longValue())
                .stakeHeldFor(cs.loadUint(32).longValue())
                .build();
    }
}
