package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams21 {
    GasLimitsPrices configGasPrices;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(configGasPrices.toCell())
                .endCell();
    }

    public static ConfigParams21 deserialize(CellSlice cs) {
        return ConfigParams21.builder()
                .configGasPrices(GasLimitsPrices.deserialize(cs))
                .build();
    }
}
