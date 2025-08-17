package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
public class BinTree implements Serializable {
  ShardDescr value;
  BinTree left;
  BinTree right;

  public BinTree() {}

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
    return addToBinTree(this);
  }

  private static Cell addToBinTree(BinTree tree) {
    CellBuilder cb = CellBuilder.beginCell();
    if (tree.value != null) {
      cb.storeBit(true);
      cb.storeRef(CellBuilder.beginCell().storeBit(false).storeCell(tree.value.toCell()).endCell());
      if (tree.left != null && tree.left.value != null) {
        cb.storeRef(addToBinTree(tree.left));
      }
      if (tree.right != null && tree.right.value != null) {
        cb.storeRef(addToBinTree(tree.right));
      }
    } else {
      cb.storeBit(false);
    }
    return cb.endCell();
  }

  public static BinTree deserialize(CellSlice cs) {
    if (cs.isExotic() || cs.getRestBits() == 0) {
      return null;
    }

    BinTree root = new BinTree();
    if (cs.loadBit()) {
      if (cs.getRefsCount() != 0) {
        CellSlice internalCs = CellSlice.beginParse(cs.loadRef());
        if (!internalCs.loadBit()) {
          root.value = ShardDescr.deserialize(internalCs);
        }
      }
      if (cs.getRefsCount() != 0) {
        root.left = deserialize(CellSlice.beginParse(cs.loadRef()));
      }
      if (cs.getRefsCount() != 0) {
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
