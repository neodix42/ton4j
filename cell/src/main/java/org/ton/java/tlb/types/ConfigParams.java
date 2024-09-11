package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;

@Builder
@Data
public class ConfigParams {
    Address configAddr;
    TonHashMap config;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeAddress(configAddr)
                .storeDict(config.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 32).endCell().getBits(),
                        v -> CellBuilder.beginCell().storeRef((Cell) v).endCell())
                )
                .endCell();
    }

    public static ConfigParams deserialize(CellSlice cs) {
        return ConfigParams.builder()
                .configAddr(Address.of(cs.loadBits(256).toByteArray())) // bounceable and workchain -1
                .config(CellSlice.beginParse(cs.loadRef()).loadDict(32, k -> k.readUint(32), v -> v))
                .build();
    }
}
