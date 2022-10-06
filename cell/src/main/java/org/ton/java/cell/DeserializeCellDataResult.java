package org.ton.java.cell;

public class DeserializeCellDataResult {
    Cell cell;
    byte[] cellsData;

    DeserializeCellDataResult(Cell cell, byte[] cellsData) {
        this.cell = cell;
        this.cellsData = cellsData;
    }
}
