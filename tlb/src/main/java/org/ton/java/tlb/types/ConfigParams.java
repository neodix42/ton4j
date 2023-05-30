package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.bitstring.BitString;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;

@Builder
@Getter
@Setter
@ToString
public class ConfigParams {
    Address configAddr;
    TonHashMap config;

    public static ConfigParams loadFromCell(CellSlice cs) {
        BitString addrBits = cs.loadBits(256);
        TonHashMap configDict = CellSlice.beginParse(cs.loadRef()).loadDict(32,
                k -> k.readUint(32),
                v -> CellSlice.beginParse(v).loadUint(8)
        );
        return ConfigParams.builder()
                .configAddr(Address.of((byte) 0, 255, addrBits.toByteArray()))
                .config(configDict)
                .build();
    }
}
