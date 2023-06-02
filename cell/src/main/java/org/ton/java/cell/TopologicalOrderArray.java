package org.ton.java.cell;

public class TopologicalOrderArray {
    int[] cellHash;
    Cell cell;

    TopologicalOrderArray(int[] cellHash, Cell cell) {
        this.cellHash = cellHash;
        this.cell = cell;
    }
}
