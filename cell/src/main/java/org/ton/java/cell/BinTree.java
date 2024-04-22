package org.ton.java.cell;

import org.ton.java.tlb.types.ShardDescr;

import java.util.*;

import static java.util.Objects.isNull;

/**
 * not finished
 */
public class BinTree {

    private Deque<ShardDescr> list;

    public BinTree(Deque<ShardDescr> list) {
        this.list = new ArrayDeque<>(list);
    }

    public Cell toCell() {
        if (list.size() == 0) {
            return CellBuilder.beginCell().endCell();
        }
        return addToBinTree(list, null);
    }

    private static Cell addToBinTree(Deque<ShardDescr> cells, Cell left) {
        CellBuilder c = CellBuilder.beginCell();
        if (cells.size() >= 1) {
            CellBuilder cb = CellBuilder.beginCell();
            cb.storeBit(true);
            cb.storeRef(CellBuilder.beginCell()
                    .storeBit(false)
                    .storeCell(isNull(left) ? cells.pop().toCell() : left)
                    .endCell());
            if (cells.size() != 0) {
                cb.storeRef(addToBinTree(cells, cells.pop().toCell()));
            }
            return cb.endCell();
        } else {
            CellBuilder cb = CellBuilder.beginCell();
            cb.storeBit(false);
            cb.storeCell(left);
            return cb.endCell();
        }
    }

    public static List<ShardDescr> deserialize(CellSlice cs) {
        if (cs.isExotic()) {
            return Collections.emptyList();
        }

        if (cs.loadBit()) {
            List<ShardDescr> l = new ArrayList<>();
            if (cs.refs.size() != 0) {
                l.addAll(deserialize(CellSlice.beginParse(cs.loadRef())));
            }
            if (cs.refs.size() != 0) {
                l.addAll(deserialize(CellSlice.beginParse(cs.loadRef())));
            }
            return l;
        } else {
            return List.of(ShardDescr.deserialize(cs));
        }
    }
}
