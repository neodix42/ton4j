package org.ton.java.cell;

import org.ton.java.tlb.types.ShardDescr;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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
        return toCellHelper(this);
    }

    private Cell toCellHelper(BinTree node) {
        if (node == null || node.value == null) {
            return CellBuilder.beginCell().endCell();
        }

        CellBuilder cb = CellBuilder.beginCell();
        cb.storeBit(true);
        cb.storeRef(CellBuilder.beginCell()
                .storeBit(false)
                .storeCell(node.value.toCell())
                .endCell());

        if (node.left != null || node.right != null) {
            cb.storeRef(addToBinTree(node.left, node.right));
        }

        return cb.endCell();
    }

    private Cell addToBinTree(BinTree left, BinTree right) {
        if (right != null) {
            CellBuilder cb = CellBuilder.beginCell();
            cb.storeBit(true);
            cb.storeRef(CellBuilder.beginCell()
                    .storeBit(false)
                    .storeCell(left != null ? toCellHelper(left) : CellBuilder.beginCell().endCell())
                    .endCell());
            cb.storeRef(toCellHelper(right));
            return cb.endCell();
        } else {
            CellBuilder cb = CellBuilder.beginCell();
            cb.storeCell(left != null ? toCellHelper(left) : CellBuilder.beginCell().endCell());
            return cb.endCell();
        }
    }

    public static BinTree deserialize(CellSlice cs) {
        if (cs.bits.getLength() == 0 || cs.isExotic()) {
            return null;
        }

        if (cs.loadBit()) {
            BinTree left = null;
            BinTree right = null;

            if (!cs.refs.isEmpty()) {
                left = deserialize(CellSlice.beginParse(cs.loadRef()));
            }
            if (!cs.refs.isEmpty()) {
                right = deserialize(CellSlice.beginParse(cs.loadRef()));
            }

            return new BinTree(null, left, right);
        } else {
            ShardDescr value = ShardDescr.deserialize(cs);
            return new BinTree(value);
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
