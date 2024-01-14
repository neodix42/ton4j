package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMap;

@Builder
@Getter
@Setter
@ToString
public class ConfigParams {
    Address configAddr;
    TonHashMap config;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeAddress(configAddr)
                .storeDict(config.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                        v -> CellBuilder.beginCell().storeRef((Cell) v))
                )
                .endCell();
    }
}
