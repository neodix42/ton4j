package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;

@Builder
@Getter
@Setter
@ToString
public class ConfigParams9 {
    TonHashMap mandatoryParams;

    public Cell toCell() {

        Cell dict;

        dict = mandatoryParams.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 32).endCell().getBits(),
                v -> CellBuilder.beginCell().storeBit((Boolean) v).endCell()
        );
        return CellBuilder.beginCell()
                .storeDict(dict)
                .endCell();
    }

    public static ConfigParams9 deserialize(CellSlice cs) {
        return ConfigParams9.builder()
                .mandatoryParams(cs.loadDict(32,
                        k -> k.readUint(32),
                        v -> CellSlice.beginParse(v).loadBit()))
                .build();
    }
}
