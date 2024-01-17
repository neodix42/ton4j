package org.ton.java.hashmaps;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.BinTree;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestBinTree {

    @Test
    public void testBinTree() {

        Cell c1 = CellBuilder.beginCell()
                .storeBit(false)
                .storeUint(42, 32)
                .endCell();
        Cell c2 = CellBuilder.beginCell()
                .storeBit(false)
                .storeUint(43, 32)
                .endCell();
        Cell c3 = CellBuilder.beginCell()
                .storeBit(true)
                .storeRef(c1)
                .storeRef(c2)
                .endCell();
        Cell c4 = CellBuilder.beginCell()
                .storeBit(true)
                .storeRef(c3)
                .storeRef(c1)
                .endCell();
        Cell c5 = CellBuilder.beginCell()
                .storeBit(true)
                .storeRef(c4)
                .endCell();

        List<Cell> bt = BinTree.deserialize(CellSlice.beginParse(c5));

        for (Cell c : bt) {
            log.info("cell: {}", CellSlice.beginParse(c).loadUint(32));
        }
    }

    @Test
    public void testBinTreeDeque() {
        Cell c1 = CellBuilder.beginCell()
                .storeUint(42, 32)
                .endCell();
        Cell c2 = CellBuilder.beginCell()
                .storeUint(43, 32)
                .endCell();
        Cell c3 = CellBuilder.beginCell()
                .storeUint(44, 32)
                .endCell();
        Cell c4 = CellBuilder.beginCell()
                .storeUint(45, 32)
                .endCell();
        Cell c5 = CellBuilder.beginCell()
                .storeUint(46, 32)
                .endCell();

        Deque<Cell> cells = new ArrayDeque<>();
        cells.add(c1);
        cells.add(c2);
        cells.add(c3);
        cells.add(c4);
        cells.add(c5);

        Cell c = BinTree.toCell(cells);

        List<Cell> bt = BinTree.deserialize(CellSlice.beginParse(c));

        for (Cell cc : bt) {
            log.info("cell: {}", CellSlice.beginParse(cc).loadUint(32));
        }

        assertThat(bt.size()).isEqualTo(5);
    }
}
