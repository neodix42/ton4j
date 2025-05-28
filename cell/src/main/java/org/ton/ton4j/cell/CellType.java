package org.ton.ton4j.cell;

public enum CellType {

    ORDINARY(-0x01),
    PRUNED_BRANCH(0x01),
    LIBRARY(0x02),
    MERKLE_PROOF(0x03),
    MERKLE_UPDATE(0x04),
    UNKNOWN(0xFF);

    private final int value;

    CellType(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }

    public int getIndex() {
        return ordinal();
    }
}
