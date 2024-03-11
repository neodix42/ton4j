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
public class ConfigParams5 {
    long magic;
    Address blackholerAddr;
    long feeBurnNum;
    long feeBurnDenom;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(01, 8)
                .storeAddress(blackholerAddr)
                .storeUint(feeBurnNum, 32)
                .storeUint(feeBurnDenom, 32)
                .endCell();
    }

    public static ConfigParams5 deserialize(CellSlice cs) {
        return ConfigParams5.builder()
                .magic(cs.loadUint(8).longValue())
                .blackholerAddr( // test maybe
                        cs.loadBit() ? Address.of(cs.loadBits(256).toByteArray()) : null) // bounceable and workchain -1
                .feeBurnNum(cs.loadUint(32).longValue())
                .feeBurnDenom(cs.loadUint(32).longValue())
                .build();
    }
}
