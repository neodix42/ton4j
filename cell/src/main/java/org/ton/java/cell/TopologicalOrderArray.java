package org.ton.java.cell;

public class TopologicalOrderArray {
    byte[] cellHash;
    Cell cell;

    TopologicalOrderArray(byte[] cellHash, Cell cell) {
        this.cellHash = cellHash;
        this.cell = cell;
    }
}
