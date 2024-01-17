package org.ton.java.cell;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static java.util.Objects.isNull;

public class BinTree {

    public static Cell toCell(Deque<Cell> l) {
        return addToBinTree(l, null);
    }

    private static Cell addToBinTree(Deque<Cell> cells, Cell left) {
        CellBuilder c = CellBuilder.beginCell();
        if (cells.size() >= 1) {
            CellBuilder cb = CellBuilder.beginCell();
            cb.storeBit(true);
            cb.storeRef(CellBuilder.beginCell()
                    .storeBit(false)
                    .storeCell(isNull(left) ? cells.pop() : left));
            cb.storeRef(addToBinTree(cells, cells.pop()));
            return cb.endCell();
        } else {
            CellBuilder cb = CellBuilder.beginCell();
            cb.storeBit(false);
            cb.storeCell(left);
            return cb.endCell();
        }
    }

    public static List<Cell> deserialize(CellSlice cs) {
        if (cs.isExotic()) {
            return List.of(cs.sliceToCell());
        }

        if (cs.loadBit()) {
            List<Cell> l = new ArrayList<>();
            if (cs.refs.size() != 0) {
                l.addAll(deserialize(CellSlice.beginParse(cs.loadRef())));
            }
            if (cs.refs.size() != 0) {
                l.addAll(deserialize(CellSlice.beginParse(cs.loadRef())));
            }
            return l;
        } else {
            return List.of(cs.sliceToCell());
        }
    }
}
