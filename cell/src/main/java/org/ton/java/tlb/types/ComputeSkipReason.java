package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
@ToString
public class ComputeSkipReason implements ComputePhase {
    String type;

    public Cell toCell() {
        switch (type) {
            case "NO_STATE" -> {
                return CellBuilder.beginCell().storeUint(0b00, 2).endCell();
            }
            case "BAD_STATE" -> {
                return CellBuilder.beginCell().storeUint(0b01, 2).endCell();
            }
            case "NO_GAS" -> {
                return CellBuilder.beginCell().storeUint(0b10, 2).endCell();
            }
            case "SUSPENDED" -> {
                return CellBuilder.beginCell().storeUint(0b110, 3).endCell();
            }
        }
        throw new Error("unknown compute skip reason");
    }

    public static ComputePhase deserialize(CellSlice cs) {
        int skipReasonFlag = cs.loadUint(2).intValue();

        switch (skipReasonFlag) {
            case 0b00 -> {
                return ComputeSkipReason.builder().type("NO_STATE").build();
            }
            case 0b01 -> {
                return ComputeSkipReason.builder().type("BAD_STATE").build();
            }
            case 0b10 -> {
                return ComputeSkipReason.builder().type("NO_GAS").build();
            }
            case 0b11 -> {
                boolean isNotSuspended = cs.loadBit();
                if (!isNotSuspended) {
                    return ComputeSkipReason.builder().type("SUSPENDED").build();
                }
            }
        }
        throw new Error("unknown compute skip reason, found 0x" + Integer.toBinaryString(skipReasonFlag));
    }
}