package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams4 {
    Address dnsRootAddr;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeAddress(dnsRootAddr)
                .endCell();
    }

    public static ConfigParams4 deserialize(CellSlice cs) {
        return ConfigParams4.builder()
                .dnsRootAddr(Address.of(cs.loadBits(256).toByteArray())) // bounceable and workchain -1
                .build();
    }
}
