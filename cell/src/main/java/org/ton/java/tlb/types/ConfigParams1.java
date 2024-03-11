package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
@ToString
public class ConfigParams1 {
    Address electorAddr;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeAddress(electorAddr)
                .endCell();
    }

    public static ConfigParams1 deserialize(CellSlice cs) {
        return ConfigParams1.builder()
                .electorAddr(Address.of(cs.loadBits(256).toByteArray())) // bounceable and workchain -1
                .build();
    }
}
