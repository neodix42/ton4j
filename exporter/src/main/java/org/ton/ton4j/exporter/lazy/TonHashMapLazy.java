package org.ton.ton4j.exporter.lazy;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.utils.Utils;

/** Ordinary Hashmap (Patricia Tree), with fixed length keys. */
@Data
public class TonHashMapLazy implements Serializable {

  public HashMap<Object, Object> elements;
  int keySize;

  /**
   * HashMap with the fixed length keys. TonHashMap cannot be empty. If you plan to store empty
   * Hashmap consider using TonHashMapE.
   *
   * <p>Notice, all keys should be of the same size. If you have keys of different size - align
   * them. Duplicates are not allowed.
   *
   * @param keySize key size in bits
   */
  public TonHashMapLazy(int keySize) {
    // Initialize with a reasonable initial capacity
    // Most TON dictionaries are small, so 16 is a good starting point
    elements = new LinkedHashMap<>(16, 0.75f);
    this.keySize = keySize;
  }

  public List<Node> deserializeEdge(CellSliceLazy edge, int keySize, final BitString key) {

    if (edge.type == CellType.PRUNED_BRANCH) {
      return new ArrayList<>();
    }
    List<Node> nodes = new ArrayList<>(4);
    BitString l = deserializeLabel(edge, keySize - key.getUsedBits());
    key.writeBitString(l);
    if (key.getUsedBits() == keySize) {
      Cell value = CellBuilder.beginCell().storeSliceLazy(edge.bits, edge.getHashes()).endCell();
      nodes.add(new Node(key, value));
      return nodes;
    }

    for (int j = 0; j < edge.hashes.length / 32; j++) {
      byte[] hash = Utils.slice(edge.hashes, (j * 32), 32);
      Cell refCell = edge.getRefByHash(hash);

      CellSliceLazy forkEdge = CellSliceLazy.beginParse(edge.cellDbReader, refCell);

      BitString forkKey = key.clone();
      forkKey.writeBit(j != 0);
      nodes.addAll(deserializeEdge(forkEdge, keySize, forkKey));
    }
    return nodes;
  }

  /** Loads HashMap and parses keys and values HashMap X Y; */
  void deserialize(
      CellSliceLazy c, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
    List<Node> nodes = deserializeEdge(c, keySize, new BitString(keySize));
    for (Node node : nodes) {
      elements.put(keyParser.apply(node.getKey()), valueParser.apply(node.getValue()));
    }
  }

  /**
   * Read the keys in array and return binary tree in the form of Patrcia Tree Node
   *
   * @param nodes list which contains nodes
   * @return tree node
   */
  PatriciaTreeNode splitTree(List<Node> nodes) {
    if (nodes.size() == 1) {
      return new PatriciaTreeNode("", 0, nodes.get(0), null, null);
    }

    List<Node> left = new ArrayList<>();
    List<Node> right = new ArrayList<>();

    for (Node node : nodes) {
      boolean lr = node.getKey().readBit();

      if (lr) {
        right.add(node);
      } else {
        left.add(node);
      }
    }

    PatriciaTreeNode leftNode =
        left.size() > 1
            ? splitTree(left)
            : left.isEmpty() ? null : new PatriciaTreeNode("", 0, left.get(0), null, null);
    PatriciaTreeNode rightNode =
        right.size() > 1
            ? splitTree(right)
            : right.isEmpty() ? null : new PatriciaTreeNode("", 0, right.get(0), null, null);

    return new PatriciaTreeNode("", keySize, null, leftNode, rightNode);
  }

  /**
   * Flatten binary tree (by cutting empty branches) if possible
   *
   * @param node tree node
   * @param m maximal possible length of prefix
   * @return flattened tree node
   */
  PatriciaTreeNode flatten(PatriciaTreeNode node, int m) {
    if (node == null) {
      return null;
    }

    if (node.getMaxPrefixLength() == 0) {
      node.setMaxPrefixLength(m);
    }

    if (node.getLeafNode() != null) {
      return node;
    }

    PatriciaTreeNode left = node.getLeft();
    PatriciaTreeNode right = node.getRight();

    if (left == null) {
      return flatten(
          new PatriciaTreeNode(node.getPrefix() + "1", m, null, right.getLeft(), right.getRight()),
          m);
    } else if (right == null) {
      return flatten(
          new PatriciaTreeNode(node.getPrefix() + "0", m, null, left.getLeft(), left.getRight()),
          m);
    } else {
      node.setMaxPrefixLength(m);
      node.setLeft(flatten(left, m - node.getPrefix().length() - 1));
      node.setRight(flatten(right, m - node.getPrefix().length() - 1));
      return node;
    }
  }

  private boolean isSame(String label) {
    if (label.isEmpty() || label.length() == 1) {
      return true;
    }
    for (int i = 1; i < label.length(); i++) {
      if (label.charAt(i) != label.charAt(0)) {
        return false;
      }
    }
    return true;
  }

  private int detectLabelType(String label, int keyLength) {
    int type = 0;
    // hml_short$0 {m:#} {n:#} len:(Unary ~n) {n <= m} s:(n * Bit) = HmLabel ~n m;
    // first bit for 0, then n + 1 for Unary ~n and next n is n * Bit
    // 0
    int bestLength = 1 + label.length() + 1 + label.length();

    // hml_long$10 {m:#} n:(#<= m) s:(n * Bit) = HmLabel ~n m;
    // first 1 + 1 for 10, then log2 bits for n and next n is n * Bit
    // 1
    int labelLongLength = 1 + 1 + (int) Math.ceil(log2(keyLength + 1)) + label.length();

    boolean isSame = isSame(label);
    // hml_same$11 {m:#} v:Bit n:(#<= m) = HmLabel ~n m;
    // 2
    int labelSameLength = 1 + 1 + 1 + (int) Math.ceil(log2(keyLength + 1));

    if (labelLongLength < bestLength) {
      type = 1;
      bestLength = labelLongLength;
    }
    if (isSame) {
      if (labelSameLength < bestLength) {
        type = 2;
      }
    }
    return type;
  }

  /**
   * Serialize HashMap label hml_short$0 {m:#} {n:#} len:(Unary ~n) {n <= m} s:(n * Bit) = HmLabel
   * ~n m; hml_long$10 {m:#} n:(#<= m) s:(n * Bit) = HmLabel ~n m; hml_same$11 {m:#} v:Bit n:(#<= m)
   * = HmLabel ~n m;
   *
   * @param label String label string of zeroes and ones "010101"
   * @param m int maximal possible length of the label
   * @param builder Cell to which label will be serialized
   */
  public void serialize_label(String label, int m, CellBuilder builder) {
    int t = detectLabelType(label, m);
    int sizeOfM = BigInteger.valueOf(m).bitLength();
    if (t == 0) {
      int n = label.length();
      builder.storeBit(false); // hml_short
      for (int i = 0; i < n; i++) {
        builder.storeBit(true); // Unary n
      }
      builder.storeBit(false); // Unary 0
      for (Character c : label.toCharArray()) {
        builder.storeBit(c == '1');
      }
    } else if (t == 1) {
      builder.storeBit(true);
      builder.storeBit(false); // hml_long
      builder.storeUint(label.length(), sizeOfM);
      for (Character c : label.toCharArray()) {
        builder.storeBit(c == '1');
      }
    } else if (t == 2) {
      builder.storeBit(true);
      builder.storeBit(true); // hml_same
      builder.storeBit(label.charAt(0) == '1');
      builder.storeUint(label.length(), sizeOfM);
    } else {
      throw new IllegalStateException("Unknown label type: " + t);
    }
  }

  /**
   * Serialize HashMap edge hm_edge#_ {n:#} {X:Type} {l:#} {m:#} label:(HmLabel ~l n) {n = (~m) + l}
   * node:(HashmapNode m X) = Hashmap n X;
   *
   * @param node tree node which contains [label as "0" and "1" string, maximal possible size of
   *     label, leaf or left fork, right fork]
   * @param builder Cell to which edge will be serialized
   */
  public void serialize_edge(PatriciaTreeNode node, CellBuilder builder) {
    if (node == null) {
      return;
    }
    if (node.getLeafNode() != null) { // contains leaf
      BitString bs =
          node.getLeafNode().getKey().readBits(node.getLeafNode().getKey().getUsedBits());
      node.setPrefix(bs.toBitString());
      serialize_label(node.getPrefix(), node.getMaxPrefixLength(), builder);
      builder.storeCell(node.getLeafNode().getValue());
    } else { // contains fork
      serialize_label(node.getPrefix(), node.getMaxPrefixLength(), builder);
      CellBuilder leftCell = CellBuilder.beginCell();
      serialize_edge(node.getLeft(), leftCell);
      CellBuilder rightCell = CellBuilder.beginCell();
      serialize_edge(node.getRight(), rightCell);
      builder.storeRef(leftCell.endCell());
      builder.storeRef(rightCell.endCell());
    }
  }

  public Cell serialize(Function<Object, BitString> keyParser, Function<Object, Cell> valueParser) {
    List<Node> nodes = new ArrayList<>();
    for (Map.Entry<Object, Object> entry : elements.entrySet()) {
      BitString key = keyParser.apply(entry.getKey());
      Cell value = valueParser.apply(entry.getValue());
      nodes.add(new Node(key, value));
    }

    if (nodes.isEmpty()) {
      throw new Error("TonHashMap does not support empty dict. Consider using TonHashMapE");
    }

    PatriciaTreeNode root = flatten(splitTree(nodes), keySize);
    CellBuilder b = CellBuilder.beginCell();
    serialize_edge(root, b);

    return b.endCell();
  }

  /**
   * Deserialize label
   *
   * @param edge cell
   * @param m length at most possible bits of n (key)
   */
  public BitString deserializeLabel(CellSliceLazy edge, int m) {
    if (!edge.loadBit()) {
      // hml_short$0 {m:#} {n:#} len:(Unary ~n) s:(n * Bit) = HmLabel ~n m;
      return deserializeLabelShort(edge);
    }
    if (!edge.loadBit()) {
      // hml_long$10 {m:#} n:(#<= m) s:(n * Bit) = HmLabel ~n m;
      return deserializeLabelLong(edge, m);
    }
    // hml_same$11 {m:#} v:Bit n:(#<= m) = HmLabel ~n m;
    return deserializeLabelSame(edge, m);
  }

  private BitString deserializeLabelShort(CellSliceLazy edge) {
    // Find the length by counting consecutive 1s until we hit a 0
    int length = 0;
    while (length < edge.getRestBits() && edge.preloadBitAt(length + 1)) {
      length++;
    }
    edge.skipBits(length + 1);
    return edge.loadBits(length);
  }

  private BitString deserializeLabelLong(CellSliceLazy edge, int m) {
    BigInteger length = edge.loadUint((int) Math.ceil(log2((m + 1))));
    return edge.loadBits(length.intValue());
  }

  private BitString deserializeLabelSame(CellSliceLazy edge, int m) {
    boolean v = edge.loadBit();
    BigInteger length = edge.loadUint((int) Math.ceil(log2((m + 1))));
    BitString r = new BitString(length.intValue());
    for (int i = 0; i < length.intValue(); i++) {
      r.writeBit(v);
    }
    return r;
  }

  // Cache log(2) value to avoid recalculating it
  private static final double LOG_2 = Math.log(2);

  /**
   * Optimized log2 calculation This is used frequently in dictionary operations
   *
   * @param n The input value
   * @return log base 2 of n
   */
  private static double log2(int n) {
    // Fast path for powers of 2
    if ((n & (n - 1)) == 0) {
      return Integer.numberOfTrailingZeros(n);
    }
    return Math.log(n) / LOG_2;
  }

  /**
   * Get a key by its index in the map Optimized version that uses an array for faster access
   *
   * @param index The index of the key to retrieve
   * @return The key at the specified index
   */
  public Object getKeyByIndex(long index) {
    if (index < 0 || index >= elements.size()) {
      throw new Error("key not found at index " + index);
    }

    // Convert to array for faster indexed access
    Object[] keys = elements.keySet().toArray();
    if (index < keys.length) {
      return keys[(int) index];
    }

    throw new Error("key not found at index " + index);
  }

  /**
   * Get a value by its index in the map Optimized version that uses an array for faster access
   *
   * @param index The index of the value to retrieve
   * @return The value at the specified index
   */
  public Object getValueByIndex(long index) {
    if (index < 0 || index >= elements.size()) {
      throw new Error("value not found at index " + index);
    }

    // Convert to array for faster indexed access
    Object[] values = elements.values().toArray();
    if (index < values.length) {
      return values[(int) index];
    }

    throw new Error("value not found at index " + index);
  }

  private int readUnaryLength(CellSlice slice) {
    int res = 0;
    while (slice.loadBit()) {
      res++;
    }
    return res;
  }

  private Cell endExoticCell(CellBuilder builder, CellType type) {
    Cell c = builder.endCell();
    Cell exotic = new Cell(c.getBits(), c.getBitLength(), c.getRefs(), true, type);
    exotic.calculateHashes();
    return exotic;
  }

  private Cell convertToMerkleProof(Cell c) {
    return endExoticCell(
        CellBuilder.beginCell()
            .storeUint(3, 8)
            .storeBytes(c.getHash(0))
            .storeUint(c.getDepthLevels()[0], 16)
            .storeRef(c),
        CellType.MERKLE_PROOF);
  }

  private Cell convertToPrunedBranch(Cell c) {
    return endExoticCell(
        CellBuilder.beginCell()
            .storeUint(1, 8)
            .storeUint(1, 8)
            .storeBytes(c.getHash(0))
            .storeUint(c.getDepthLevels()[0], 16),
        CellType.PRUNED_BRANCH);
  }

  private String padString(String original, int requiredSize, char padChar) {
    if (original.length() >= requiredSize) {
      return original; // No padding needed
    }

    return StringUtils.repeat(padChar, Math.max(0, requiredSize - original.length() + 1))
        + original;
  }
}
