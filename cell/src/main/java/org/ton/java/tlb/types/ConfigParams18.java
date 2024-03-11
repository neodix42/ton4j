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
public class ConfigParams18 {
    TonHashMap storagePrices;

    public Cell toCell() {

        Cell dict;

        dict = storagePrices.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                v -> CellBuilder.beginCell().storeCell(((StoragePrices) v).toCell())
        );
        return CellBuilder.beginCell()
                .storeDict(dict)
                .endCell();
    }

    public static ConfigParams18 deserialize(CellSlice cs) {
        return ConfigParams18.builder()
                .storagePrices(cs.loadDict(32,
                        k -> k.readUint(32),
                        v -> StoragePrices.deserialize(cs)))
                .build();
    }
}
