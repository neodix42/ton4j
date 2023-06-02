package org.ton.java.cell;

public class DeserializeCellDataResult {
    Cell cell;
    int[] cellsData;

    DeserializeCellDataResult(Cell cell, int[] cellsData) {
        this.cell = cell;
        this.cellsData = cellsData;
    }
}
