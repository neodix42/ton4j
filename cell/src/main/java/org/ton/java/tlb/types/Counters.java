package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 counters#_ last_updated:uint32 total:uint64 cnt2048:uint64 cnt65536:uint64 = Counters;
 */
public class Counters {

    long lastUpdated;
    BigInteger total;
    BigInteger cnt2048;
    BigInteger cnt65536;


    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(lastUpdated, 32)
                .storeUint(total, 64)
                .storeUint(cnt2048, 64)
                .storeUint(cnt65536, 64)
                .endCell();
    }

    public static Counters deserialize(CellSlice cs) {

        return Counters.builder()
                .lastUpdated(cs.loadUint(32).longValue())
                .total(cs.loadUint(64))
                .cnt2048(cs.loadUint(64))
                .cnt65536(cs.loadUint(64))
                .build();
    }
}
