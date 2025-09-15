package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.util.*;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Data
public class BinTree implements Serializable {
  private Map<String, HashmapKV> storage;

  public BinTree() {
    this.storage = new HashMap<>();
  }

  public BinTree(Map<String, HashmapKV> storage) {
    this.storage = storage != null ? storage : new HashMap<>();
  }

  public BinTree(ShardDescr value, BinTree left, BinTree right) {
    this.storage = new HashMap<>();
    // Convert traditional tree structure to hash map storage for backward compatibility
    if (value != null || left != null || right != null) {
      convertTreeToStorage(value, left, right, "");
    }
  }

  @Data
  public static class HashmapKV {
    private final Cell key;
    private final Cell value;

    public HashmapKV(Cell key, Cell value) {
      this.key = key;
      this.value = value;
    }
  }

  private void convertTreeToStorage(ShardDescr value, BinTree left, BinTree right, String keyPath) {
    if (value != null) {
      // This is a leaf node
      Cell keyCell = createKeyCell(keyPath);
      storage.put(bytesToHex(keyCell.hash()), new HashmapKV(keyCell, value.toCell()));
    } else if (left != null || right != null) {
      // This is a fork node, process children
      if (left != null) {
        String leftPath = keyPath + "0";
        for (Map.Entry<String, HashmapKV> entry : left.getAllEntries().entrySet()) {
          HashmapKV kv = entry.getValue();
          String existingPath = extractPathFromKey(kv.getKey());
          String newPath = leftPath + existingPath;
          Cell newKeyCell = createKeyCell(newPath);
          storage.put(bytesToHex(newKeyCell.hash()), new HashmapKV(newKeyCell, kv.getValue()));
        }
      }
      if (right != null) {
        String rightPath = keyPath + "1";
        for (Map.Entry<String, HashmapKV> entry : right.getAllEntries().entrySet()) {
          HashmapKV kv = entry.getValue();
          String existingPath = extractPathFromKey(kv.getKey());
          String newPath = rightPath + existingPath;
          Cell newKeyCell = createKeyCell(newPath);
          storage.put(bytesToHex(newKeyCell.hash()), new HashmapKV(newKeyCell, kv.getValue()));
        }
      }
    }
  }

  private Cell createKeyCell(String binaryPath) {
    CellBuilder cb = CellBuilder.beginCell();
    for (char c : binaryPath.toCharArray()) {
      cb.storeBit(c == '1');
    }
    return cb.endCell();
  }

  private String extractPathFromKey(Cell keyCell) {
    CellSlice keySlice = CellSlice.beginParse(keyCell);
    StringBuilder path = new StringBuilder();
    while (keySlice.getRestBits() > 0) {
      path.append(keySlice.loadBit() ? "1" : "0");
    }
    return path.toString();
  }

  public static BinTree fromDeque(Deque<ShardDescr> cells) {
    return buildTree(cells);
  }

  private static BinTree buildTree(Deque<ShardDescr> cells) {
    if (cells.isEmpty()) {
      return null;
    }
    
    BinTree tree = new BinTree();
    int index = 0;
    
    // Convert deque to array for easier indexing
    ShardDescr[] array = cells.toArray(new ShardDescr[0]);
    
    // Build tree using array indices
    buildTreeRecursive(tree, array, index, "");
    
    return tree;
  }
  
  private static void buildTreeRecursive(BinTree tree, ShardDescr[] array, int index, String keyPath) {
    if (index >= array.length) {
      return;
    }
    
    // Store current element as leaf
    ShardDescr value = array[index];
    Cell keyCell = tree.createKeyCell(keyPath);
    tree.storage.put(tree.bytesToHex(keyCell.hash()), new HashmapKV(keyCell, value.toCell()));
    
    // Build left and right subtrees
    int leftIndex = 2 * index + 1;
    int rightIndex = 2 * index + 2;
    
    if (leftIndex < array.length) {
      buildTreeRecursive(tree, array, leftIndex, keyPath + "0");
    }
    
    if (rightIndex < array.length) {
      buildTreeRecursive(tree, array, rightIndex, keyPath + "1");
    }
  }

  public Cell toCell() {
    if (storage.isEmpty()) {
      return CellBuilder.beginCell().endCell();
    }
    
    // Build tree structure from hash map entries
    TreeNode root = buildTreeFromStorage();
    return serializeTreeNode(root);
  }

  private TreeNode buildTreeFromStorage() {
    if (storage.isEmpty()) {
      return null;
    }

    // If we have only one element with empty path, it's a single leaf
    if (storage.size() == 1) {
      for (HashmapKV kv : storage.values()) {
        String path = extractPathFromKey(kv.getKey());
        if (path.isEmpty()) {
          TreeNode leafNode = new TreeNode();
          leafNode.isLeaf = true;
          leafNode.value = ShardDescr.deserialize(CellSlice.beginParse(kv.getValue()));
          return leafNode;
        }
      }
    }

    // Build tree recursively from root
    return buildTreeNodeRecursive("");
  }

  private TreeNode buildTreeNodeRecursive(String pathPrefix) {
    // Check if there's a leaf at this exact path
    TreeNode leafAtThisPath = null;
    for (Map.Entry<String, HashmapKV> entry : storage.entrySet()) {
      String path = extractPathFromKey(entry.getValue().getKey());
      if (path.equals(pathPrefix)) {
        leafAtThisPath = new TreeNode();
        leafAtThisPath.isLeaf = true;
        leafAtThisPath.value = ShardDescr.deserialize(CellSlice.beginParse(entry.getValue().getValue()));
        break;
      }
    }

    // Check if there are any children with this prefix
    boolean hasLeftChild = false;
    boolean hasRightChild = false;
    
    for (Map.Entry<String, HashmapKV> entry : storage.entrySet()) {
      String path = extractPathFromKey(entry.getValue().getKey());
      if (path.startsWith(pathPrefix) && path.length() > pathPrefix.length()) {
        char nextBit = path.charAt(pathPrefix.length());
        if (nextBit == '0') {
          hasLeftChild = true;
        } else if (nextBit == '1') {
          hasRightChild = true;
        }
      }
    }

    // If this is a leaf node and has no children, return the leaf
    if (leafAtThisPath != null && !hasLeftChild && !hasRightChild) {
      return leafAtThisPath;
    }

    // If this has children, create a fork node
    if (hasLeftChild || hasRightChild) {
      TreeNode forkNode = new TreeNode();
      forkNode.isLeaf = false;
      
      if (hasLeftChild) {
        forkNode.left = buildTreeNodeRecursive(pathPrefix + "0");
      }
      
      if (hasRightChild) {
        forkNode.right = buildTreeNodeRecursive(pathPrefix + "1");
      }
      
      return forkNode;
    }

    // Return null if no leaf and no children (empty subtree)
    return null;
  }

  private Cell serializeTreeNode(TreeNode node) {
    if (node == null) {
      return CellBuilder.beginCell().endCell();
    }

    CellBuilder cb = CellBuilder.beginCell();
    
    if (node.isLeaf) {
      // bt_leaf$0: store bit 0 followed by ShardDescr data
      cb.storeBit(false);
      cb.storeCell(node.value.toCell());
    } else {
      // bt_fork$1: store bit 1 followed by left and right references
      cb.storeBit(true);
      // Always store both references, even if null (as empty cells)
      cb.storeRef(node.left != null ? serializeTreeNode(node.left) : CellBuilder.beginCell().endCell());
      cb.storeRef(node.right != null ? serializeTreeNode(node.right) : CellBuilder.beginCell().endCell());
    }
    
    return cb.endCell();
  }

  private static class TreeNode {
    boolean isLeaf;
    ShardDescr value;
    TreeNode left;
    TreeNode right;
  }

  public static BinTree deserialize(CellSlice cs) {
    if (cs.isExotic() || cs.getRestBits() == 0) {
      return null;
    }

    BinTree tree = new BinTree();
    try {
      tree.loadFromCell(cs, "");
      return tree;
    } catch (Exception e) {
      return null;
    }
  }

  private void loadFromCell(CellSlice cs, String keyPath) throws Exception {
    if (cs.getRestBits() == 0) {
      return;
    }

    int typeFlag = cs.loadUint(1).intValue();
    
    if (typeFlag == 0) {
      // bt_leaf$0: leaf node with ShardDescr
      Cell finalKey = createKeyCell(keyPath);
      ShardDescr shardDescr = ShardDescr.deserialize(cs);
      storage.put(bytesToHex(finalKey.hash()), new HashmapKV(finalKey, shardDescr.toCell()));
    } else {
      // bt_fork$1: fork node with left and right references
      if (cs.getRefsCount() < 2) {
        throw new Exception("Fork node must have 2 references");
      }
      
      Cell leftRef = cs.loadRef();
      Cell rightRef = cs.loadRef();
      
      // Always process both subtrees, even if they appear empty
      // This is crucial for maintaining the correct tree structure
      loadFromCell(CellSlice.beginParse(leftRef), keyPath + "0");
      loadFromCell(CellSlice.beginParse(rightRef), keyPath + "1");
    }
  }

  public List<ShardDescr> toList() {
    List<ShardDescr> list = new ArrayList<>();
    for (HashmapKV kv : storage.values()) {
      ShardDescr shardDescr = ShardDescr.deserialize(CellSlice.beginParse(kv.getValue()));
      list.add(shardDescr);
    }
    return list;
  }

  public ShardDescr get(Cell key) {
    HashmapKV kv = storage.get(bytesToHex(key.hash()));
    if (kv != null) {
      return ShardDescr.deserialize(CellSlice.beginParse(kv.getValue()));
    }
    return null;
  }

  public Map<String, HashmapKV> getAllEntries() {
    return storage;
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }
}
