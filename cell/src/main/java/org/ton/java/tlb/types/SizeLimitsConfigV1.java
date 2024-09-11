package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class SizeLimitsConfigV1 implements SizeLimitsConfig {
    int magic;
    long maxMsgBits;
    long maxMsgCells;
    long maxLibraryCells;
    int maxVmDataDepth;
    long maxExtMsgSize;
    int maxExtMsgDepth;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x01, 8)
                .storeUint(maxMsgBits, 32)
                .storeUint(maxMsgCells, 32)
                .storeUint(maxLibraryCells, 32)
                .storeUint(maxVmDataDepth, 16)
                .storeUint(maxExtMsgSize, 32)
                .storeUint(maxExtMsgDepth, 16)
                .endCell();
    }

    public static SizeLimitsConfigV1 deserialize(CellSlice cs) {
        return SizeLimitsConfigV1.builder()
                .magic(cs.loadUint(8).intValue())
                .maxMsgBits(cs.loadUint(32).longValue())
                .maxMsgCells(cs.loadUint(32).longValue())
                .maxLibraryCells(cs.loadUint(32).longValue())
                .maxVmDataDepth(cs.loadUint(32).intValue())
                .maxExtMsgSize(cs.loadUint(32).longValue())
                .maxExtMsgDepth(cs.loadUint(32).intValue())
                .build();
    }
}
