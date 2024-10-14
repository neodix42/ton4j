package org.ton.java.cell;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.bitstring.BitString;

/** Ordinary Hashmap (Patricia Tree), with fixed length keys. */
public class TonHashMap {

  public HashMap<Object, Object> elements;
  int keySize;
  int maxMembers;

  /**
   * TonHashMap with the fixed length keys. TonHashMap cannot be empty. If you plan to store empty
   * Hashmap consider using TonHashMapE.
   *
   * <p>TonHashMap consists of two subsequent refs Notice, all keys should be of the same size. If
   * you have keys of different size - align them first. Duplicates are not allowed.
   *
   * @param keySize key size in bits
   * @param maxMembers max number of hashmap entries
   */
  public TonHashMap(int keySize, int maxMembers) {
    elements = new LinkedHashMap<>();
    this.keySize = keySize;
    this.maxMembers = maxMembers;
  }

  /**
   * HashMap with the fixed length keys. TonHashMap cannot be empty. If you plan to store empty
   * Hashmap consider using TonHashMapE.
   *
   * <p>Notice, all keys should be of the same size. If you have keys of different size - align
   * them. Duplicates are not allowed.
   *
   * @param keySize key size in bits
   */
  public TonHashMap(int keySize) {
    elements = new LinkedHashMap<>();
    this.keySize = keySize;
    this.maxMembers = 10000;
  }

  public List<Node> deserializeEdge(CellSlice edge, int keySize, final BitString key) {
    List<Node> nodes = new ArrayList<>();
    BitString l = deserializeLabel(edge, keySize - key.toBitString().length());
    key.writeBitString(l);
    if (key.toBitString().length() == keySize) {
      Cell value = CellBuilder.beginCell().storeSlice(edge).endCell();
      nodes.add(new Node(key, value));
      return nodes;
    }

    for (int j = 0; j < edge.refs.size(); j++) {
      CellSlice forkEdge = CellSlice.beginParse(edge.refs.get(j));
      BitString forkKey = key.clone();
      forkKey.writeBit(j != 0);
      nodes.addAll(deserializeEdge(forkEdge, keySize, forkKey));
    }
    return nodes;
  }

  /** Loads HashMap and parses keys and values HashMap X Y; */
  void deserialize(
      CellSlice c, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
    List<Node> nodes = deserializeEdge(c, keySize, new BitString(keySize));
    for (Node node : nodes) {
      elements.put(keyParser.apply(node.key), valueParser.apply(node.value));
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
      boolean lr = node.key.readBit();

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

    if (node.maxPrefixLength == 0) {
      node.maxPrefixLength = m;
    }

    if (node.leafNode != null) {
      return node;
    }

    PatriciaTreeNode left = node.left;
    PatriciaTreeNode right = node.right;

    if (left == null) {
      return flatten(new PatriciaTreeNode(node.prefix + "1", m, null, right.left, right.right), m);
    } else if (right == null) {
      return flatten(new PatriciaTreeNode(node.prefix + "0", m, null, left.left, left.right), m);
    } else {
      node.maxPrefixLength = m;
      node.left = flatten(left, m - node.prefix.length() - 1);
      node.right = flatten(right, m - node.prefix.length() - 1);
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
  void serialize_label(String label, int m, CellBuilder builder) {
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
  void serialize_edge(PatriciaTreeNode node, CellBuilder builder) {
    if (node == null) {
      return;
    }
    if (node.leafNode != null) { // contains leaf
      BitString bs = node.leafNode.key.readBits(node.leafNode.key.getUsedBits());
      node.prefix = bs.toBitString();
      serialize_label(node.prefix, node.maxPrefixLength, builder);
      builder.storeCell(node.leafNode.value);
    } else { // contains fork
      serialize_label(node.prefix, node.maxPrefixLength, builder);
      CellBuilder leftCell = CellBuilder.beginCell();
      serialize_edge(node.left, leftCell);
      CellBuilder rightCell = CellBuilder.beginCell();
      serialize_edge(node.right, rightCell);
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
  public BitString deserializeLabel(CellSlice edge, int m) {
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

  private BitString deserializeLabelShort(CellSlice edge) {
    int length = edge.bits.getBitString().indexOf("0");
    edge.skipBits(length + 1);
    return edge.loadBits(length);
  }

  private BitString deserializeLabelLong(CellSlice edge, int m) {
    BigInteger length = edge.loadUint((int) Math.ceil(log2((m + 1))));
    return edge.loadBits(length.intValue());
  }

  private BitString deserializeLabelSame(CellSlice edge, int m) {
    boolean v = edge.loadBit();
    BigInteger length = edge.loadUint((int) Math.ceil(log2((m + 1))));
    BitString r = new BitString(length.intValue());
    for (int i = 0; i < length.intValue(); i++) {
      r.writeBit(v);
    }
    return r;
  }

  private static double log2(int n) {
    return (Math.log(n) / Math.log(2));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for (Map.Entry<Object, Object> entry : elements.entrySet()) {
      String s = String.format("[%s,%s],", entry.getKey(), entry.getValue());
      sb.append(s);
    }
    if (!elements.isEmpty()) {
      sb.setLength(sb.length() - 1);
    }
    sb.append(")");
    return sb.toString();
  }

  public Object getKeyByIndex(long index) {
    long i = 0;
    for (Map.Entry<Object, Object> entry : elements.entrySet()) {
      if (i++ == index) {
        return entry.getKey();
      }
    }
    throw new Error("key not found at index " + index);
  }

  public Object getValueByIndex(long index) {
    long i = 0;
    for (Map.Entry<Object, Object> entry : elements.entrySet()) {
      if (i++ == index) {
        return entry.getValue();
      }
    }
    throw new Error("value not found at index " + index);
  }

  public Cell buildMerkleProof(
      Object key,
      Function<Object, BitString> keySerializer,
      Function<Object, Cell> valueSerializer) {
    Cell dictCell =
        CellBuilder.beginCell()
            .storeDictInLine(this.serialize(keySerializer, valueSerializer))
            .endCell();

    Cell cell =
        generateMerkleProof(
            "",
            CellSlice.beginParse(dictCell),
            keySize,
            padString(keySerializer.apply(key).toBitString(), keySize, '0'));

    return convertToMerkleProof(cell);
  }

  private int readUnaryLength(CellSlice slice) {
    int res = 0;
    while (slice.loadBit()) {
      res++;
    }
    return res;
  }

  private Cell generateMerkleProof(String prefix, CellSlice slice, int n, String key) {
    Cell originalCell = CellBuilder.beginCell().storeSlice(slice).endCell();

    int lb0 = slice.loadBit() ? 1 : 0;
    int prefixLength;
    StringBuilder pp = new StringBuilder(prefix);

    if (lb0 == 0) {
      // Short label detected
      prefixLength = readUnaryLength(slice);
      for (int i = 0; i < prefixLength; i++) {
        pp.append(slice.loadBit() ? '1' : '0');
      }
    } else {
      int lb1 = slice.loadBit() ? 1 : 0;
      if (lb1 == 0) {
        // Long label detected
        prefixLength = slice.loadUint((int) Math.ceil(Math.log(n + 1) / Math.log(2))).intValue();
        for (int i = 0; i < prefixLength; i++) {
          pp.append(slice.loadBit() ? '1' : '0');
        }
      } else {
        // Same label detected
        char bit = slice.loadBit() ? '1' : '0';
        prefixLength = slice.loadUint((int) (Math.ceil(Math.log(n + 1) / Math.log(2)))).intValue();
        for (int i = 0; i < prefixLength; i++) {
          pp.append(bit);
        }
      }
    }

    if (n - prefixLength == 0) {
      return originalCell;
    } else {
      CellSlice sl = CellSlice.beginParse(originalCell);
      Cell left = sl.loadRef();
      Cell right = sl.loadRef();
      // NOTE: Left and right branches implicitly contain prefixes '0' and '1'

      if (left.getCellType() == CellType.ORDINARY) {
        left =
            (pp.toString() + '0').equals(key.substring(0, pp.length() + 1))
                ? generateMerkleProof(
                    pp.toString() + '0', CellSlice.beginParse(left), n - prefixLength - 1, key)
                : convertToPrunedBranch(left);
      }

      if (right.getCellType() == CellType.ORDINARY) {
        right =
            (pp.toString() + '1').equals(key.substring(0, pp.length() + 1))
                ? generateMerkleProof(
                    pp.toString() + '1', CellSlice.beginParse(right), n - prefixLength - 1, key)
                : convertToPrunedBranch(right);
      }

      return CellBuilder.beginCell().storeSlice(sl).storeRef(left).storeRef(right).endCell();
    }
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
