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

    BinTree tree = new BinTree();
    if (cs.loadBit()) {
      if (cs.getRefsCount() != 0) {
        tree.left = deserialize(CellSlice.beginParse(cs.loadRef()));
      }
      if (cs.getRefsCount() != 0) {
        tree.right = deserialize(CellSlice.beginParse(cs.loadRef()));
      }
      return tree;
    } else {
      tree.value = ShardDescr.deserialize(cs);
      return tree;
    }
  }

  public List<ShardDescr> toList() {
    List<ShardDescr> list = new ArrayList<>();
    // Start with root shard ID: 0x8000000000000000
    addToListWithShardId(this, list, 0x8000000000000000L, 0);
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

  /**
   * Traverses the BinTree and computes shard IDs based on the tree structure.
   * The shard ID is computed by tracking the path through the tree:
   * - Root starts at 0x8000000000000000
   * - Going left subtracts delta, going right adds delta
   * - Delta = 0x4000000000000000 >> depth
   *
   * @param node Current node
   * @param list Result list
   * @param shardId Current shard ID
   * @param depth Current depth in the tree
   */
  private void addToListWithShardId(BinTree node, List<ShardDescr> list, long shardId, int depth) {
    if (node == null) {
      return;
    }

    if (node.value != null) {
      // Set the computed shard ID
      node.value.setComputedShardId(shardId);
      list.add(node.value);
    }

    // Compute delta for child nodes
    long delta = 0x4000000000000000L >>> depth;

    // Traverse left subtree (subtract delta)
    if (node.left != null) {
      addToListWithShardId(node.left, list, shardId - delta, depth + 1);
    }

    // Traverse right subtree (add delta)
    if (node.right != null) {
      addToListWithShardId(node.right, list, shardId + delta, depth + 1);
    }
  }
}
