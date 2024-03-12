package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

@Builder
@Getter
@Setter
@ToString
public class ConfigParams12 {
    TonHashMapE workchains;

    public Cell toCell() {

        return CellBuilder.beginCell()
                .storeDict(workchains.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                        v -> CellBuilder.beginCell().storeCell(((WorkchainDescr) v).toCell())
                ))
                .endCell();
    }

    public static ConfigParams12 deserialize(CellSlice cs) {
        return ConfigParams12.builder()
                .workchains(cs.loadDictE(32,
                        k -> k.readUint(32),
                        v -> WorkchainDescr.deserialize(CellSlice.beginParse(v))))
                .build();
    }
}
