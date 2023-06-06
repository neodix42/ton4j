package org.ton.java.cell;

public class TopologicalOrderArray {
    int[] cellHash;
    CellNew cell;

    TopologicalOrderArray(int[] cellHash, CellNew cell) {
        this.cellHash = cellHash;
        this.cell = cell;
    }
}
