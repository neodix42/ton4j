package org.ton.java.cell;

import org.ton.java.tlb.types.ShardDescr;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static java.util.Objects.isNull;

public class BinTree {
    ShardDescr value;
    BinTree left;
    BinTree right;

    public BinTree() {}

    public BinTree(ShardDescr value) {
        this.value = value;
    }

    public BinTree(ShardDescr value, BinTree left, BinTree right) {
        this.value = value;
        this.left = left;
        this.right = right;
    }

    public static BinTree fromDeque(Deque<ShardDescr> cells) {
        return buildTree(cells);
    }

    private static BinTree buildTree(Deque<ShardDescr> cells) {
        if (cells.isEmpty()) {
            return null;
        }
        ShardDescr value = cells.pop();
        BinTree left = buildTree(cells);
        BinTree right = buildTree(cells);
        return new BinTree(value, left, right);
    }

    public Cell toCell() {
        if (this.value == null && this.left == null && this.right == null) {
            return CellBuilder.beginCell().endCell();
        }
        List<ShardDescr> listView = this.toList();
        return addToBinTree(listView, null);
    }

    private static Cell addToBinTree(List<ShardDescr> cells, Cell left) {
        if (!cells.isEmpty()) {
            CellBuilder cb = CellBuilder.beginCell();
            cb.storeBit(true);
            cb.storeRef(CellBuilder.beginCell()
                    .storeBit(false)
                    .storeCell(isNull(left) ? cells.remove(0).toCell() : left)
                    .endCell());
            if (!cells.isEmpty()) {
                cb.storeRef(addToBinTree(cells, cells.remove(0).toCell()));
            }
            return cb.endCell();
        } else {
            CellBuilder cb = CellBuilder.beginCell();
            cb.storeBit(false);
            cb.storeCell(left);
            return cb.endCell();
        }
    }

    public static BinTree deserialize(CellSlice cs) {
        if (cs.isExotic() || cs.bits.getLength() == 0) {
            return null;
        }

        BinTree root = new BinTree();
        if (cs.loadBit() || !cs.refs.isEmpty()) {
            if (!cs.refs.isEmpty()) {
                root.left = deserialize(CellSlice.beginParse(cs.loadRef()));
            }
            if (!cs.refs.isEmpty()) {
                root.right = deserialize(CellSlice.beginParse(cs.loadRef()));
            }
            return root;
        } else {
            root.value = ShardDescr.deserialize(cs);
            return root;
        }
    }

    public List<ShardDescr> toList() {
        List<ShardDescr> list = new ArrayList<>();
        addToList(this, list);
        return list;
    }

    private void addToList(BinTree node, List<ShardDescr> list) {
        if (node == null) {
            return;
        }

        if (node.value != null) {
            list.add(node.value);
        }

        addToList(node.left, list);
        addToList(node.right, list);
    }
}
