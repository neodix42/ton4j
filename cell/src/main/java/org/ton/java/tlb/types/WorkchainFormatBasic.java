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
public class WorkchainFormatBasic implements WorkchainFormat {
    int wfmtBasic;
    long vmVersion;
    BigInteger vmMode;

    public Cell toCell(boolean basic) {

        if (basic) {

            return CellBuilder.beginCell()
                    .storeUint(1, 4)
                    .storeUint(vmVersion, 32)
                    .storeUint(vmMode, 64)
                    .endCell();
        }
        return null;
    }

    public static WorkchainFormatBasic deserialize(CellSlice cs) {
        return WorkchainFormatBasic.builder()
                .wfmtBasic(cs.loadUint(4).intValue())
                .vmVersion(cs.loadUint(32).longValue())
                .vmMode(cs.loadUint(64))
                .build();
    }
}
