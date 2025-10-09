package org.ton.ton4j.exporter.lazy;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.utils.Utils;

@Slf4j
@Data
public class TonHashMapAugLazy implements Serializable {

  public HashMap<Object, ValueExtra> elements; // Pair<Value,Extra>
  int keySize;

  public TonHashMapAugLazy(int keySize) {
    elements = new LinkedHashMap<>();
    this.keySize = keySize;
  }

  public List<Node> deserializeEdge(CellSliceLazy edge, int keySize, final BitString key) {
    if (edge.type == CellType.PRUNED_BRANCH) {
      //      System.out.println("TonHashMapAug: pruned branch in cell");
      return new ArrayList<>();
    }
    List<Node> nodes = new ArrayList<>();
    int m = keySize - key.getUsedBits();
    BitString l = deserializeLabel(edge, m);
    key.writeBitString(l);
    if (key.getUsedBits() == keySize) {
      Cell valueAndExtra =
          CellBuilder.beginCell().storeSliceLazy(edge.bits, edge.getHashes()).endCell();
      nodes.add(new Node(key, valueAndExtra)); // fork-extra does not exist in edge
      return nodes;
    }
    for (int i = 0; i < edge.hashes.length / 32; i++) {
      byte[] hash = Utils.slice(edge.hashes, (i * 32), 32);

      Cell refCell = edge.getRefByHash(hash);

      CellSliceLazy forkEdge = CellSliceLazy.beginParse(edge.cellDbReader, refCell);

      BitString forkKey = key.clone();
      forkKey.writeBit(i != 0);
      nodes.addAll(deserializeEdge(forkEdge, keySize, forkKey));
    }
    return nodes;
  }

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

  /** Loads HashMapAug and parses keys, values and extras */
  void deserialize(
      CellSliceLazy c,
      Function<BitString, Object> keyParser,
      Function<CellSliceLazy, Object> valueParser,
      Function<CellSliceLazy, Object> extraParser) {

    List<Node> nodes = deserializeEdge(c, keySize, new BitString(keySize));
    for (Node node : nodes) {
      CellSliceLazy valueAndExtra = CellSliceLazy.beginParse(c.cellDbReader, node.getValue());
      Object extra = extraParser.apply(valueAndExtra);
      Object value = valueParser.apply(valueAndExtra);
      elements.put(keyParser.apply(node.getKey()), new ValueExtra(value, extra));
    }
  }

  /**
   * Read the keys in array and return binary tree in the form of Patricia Tree Node
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

  void serialize_label(String label, int m, CellBuilder builder) {
    int n = label.length();
    if (label.isEmpty()) {
      builder.storeBit(false); // hml_short$0
      builder.storeBit(false); // Unary 0
      return;
    }

    int sizeOfM = BigInteger.valueOf(m).bitLength();
    if (n < sizeOfM) {
      builder.storeBit(false); // hml_short
      for (int i = 0; i < n; i++) {
        builder.storeBit(true); // Unary n
      }
      builder.storeBit(false); // Unary 0
      for (Character c : label.toCharArray()) {
        builder.storeBit(c == '1');
      }
      return;
    }

    boolean isSame =
        (label.equals(Utils.repeat("0", label.length()))
            || label.equals(Utils.repeat("10", label.length())));
    if (isSame) {
      builder.storeBit(true);
      builder.storeBit(true); // hml_same
      builder.storeBit(label.charAt(0) == '1');
      builder.storeUint(label.length(), sizeOfM);
    } else {
      builder.storeBit(true);
      builder.storeBit(false); // hml_long
      builder.storeUint(label.length(), sizeOfM);
      for (Character c : label.toCharArray()) {
        builder.storeBit(c == '1');
      }
    }
  }

  void serialize_edge(
      PatriciaTreeNode node, CellBuilder builder, BiFunction<Object, Object, Object> forkExtra) {
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
      serialize_edge(node.getLeft(), leftCell, forkExtra);
      CellBuilder rightCell = CellBuilder.beginCell();
      serialize_edge(node.getRight(), rightCell, forkExtra);
      builder.storeCell(
          ((CellBuilder) forkExtra.apply(leftCell.endCell(), rightCell.endCell())).endCell());
      builder.storeRef(leftCell.endCell());
      builder.storeRef(rightCell.endCell());
    }
  }

  /**
   * Serializes edges and puts values into fork-nodes according to forkExtra function logic
   *
   * @param keyParser - used on key
   * @param valueParser - used on every leaf
   * @param extraParser - used on every leaf
   * @param forkExtra - used only in fork-node.
   * @return Cell
   */
  public Cell serialize(
      Function<Object, BitString> keyParser,
      Function<Object, Object> valueParser,
      Function<Object, Object> extraParser,
      BiFunction<Object, Object, Object> forkExtra) {
    List<Node> nodes = new ArrayList<>();
    for (Map.Entry<Object, ValueExtra> entry : elements.entrySet()) {
      BitString key = keyParser.apply(entry.getKey());
      Cell value =
          isNull(valueParser) ? null : (Cell) valueParser.apply(entry.getValue().getValue());
      Cell extra =
          isNull(extraParser) ? null : (Cell) extraParser.apply(entry.getValue().getExtra());
      CellBuilder both = CellBuilder.beginCell();
      if (nonNull(value)) {
        both.storeSlice(CellSlice.beginParse(value));
      }
      if (nonNull(extra)) {
        both.storeSlice(CellSlice.beginParse(extra));
      }
      nodes.add(new Node(key, both.endCell()));
    }

    if (nodes.isEmpty()) {
      throw new Error("TonHashMapAug does not support empty dict. Consider using TonHashMapAugE");
    }

    PatriciaTreeNode root = flatten(splitTree(nodes), keySize);
    CellBuilder b = CellBuilder.beginCell();
    serialize_edge(root, b, forkExtra);

    return b.endCell();
  }

  private BitString deserializeLabelShort(CellSliceLazy edge) {
    // Find the length by counting consecutive 1s until we hit a 0
    int length = 0;
    while (length < edge.getRestBits() && edge.preloadBitAt(length + 1)) {
      length++;
    }
    edge.skipBits(length + 1); // Skip the unary length + terminating 0
    return edge.loadBits(length);
  }

  private BitString deserializeLabelLong(CellSliceLazy edge, int m) {
    if (m == 0) {
      return new BitString(0);
    }
    int lenBits = 32 - Integer.numberOfLeadingZeros(m);

    // Check if we have enough bits to read the length field (like C++ fetch_uint_leq)
    if (edge.getRestBits() < lenBits) {

      return new BitString(0);
    }

    BigInteger length = edge.loadUint(lenBits);

    // Validate that the length is within bounds (like C++ fetch_uint_leq validation)
    if (length.intValue() > m) {
      return new BitString(0);
    }

    // Check if we have enough bits to read the actual label data
    if (edge.getRestBits() < length.intValue()) {
      return new BitString(0);
    }

    return edge.loadBits(length.intValue());
  }

  private BitString deserializeLabelSame(CellSliceLazy edge, int m) {
    if (m == 0) {
      return new BitString(0);
    }
    boolean v = edge.loadBit();
    //    int lenBits = 32 - Integer.numberOfLeadingZeros(m);
    int lenBits = m == 0 ? 0 : 32 - Integer.numberOfLeadingZeros(m);

    // Check if we have enough bits to read the length field
    if (edge.getRestBits() < lenBits) {
      return new BitString(0);
    }

    BigInteger length = edge.loadUint(lenBits);
    if (length.intValue() > m) {
      throw new Error("Label length " + length + " exceeds maximum " + m);
    }
    BitString r = new BitString(length.intValue());
    for (int i = 0; i < length.intValue(); i++) {
      r.writeBit(v);
    }
    return r;
  }

  private static double log2(int n) {
    return (Math.log(n) / Math.log(2));
  }

  public Object getKeyByIndex(long index) {
    long i = 0;
    for (Map.Entry<Object, ValueExtra> entry : elements.entrySet()) {
      if (i == index) {
        return entry.getKey();
      }
    }
    throw new Error("key not found at index " + index);
  }

  public Object getValueByIndex(long index) {
    long i = 0;
    for (Map.Entry<Object, ValueExtra> entry : elements.entrySet()) {
      if (i++ == index) {
        return entry.getValue().getValue();
      }
    }
    throw new Error("value not found at index " + index);
  }

  public Object getEdgeByIndex(long index) {
    long i = 0;
    for (Map.Entry<Object, ValueExtra> entry : elements.entrySet()) {
      if (i++ == index) {
        return entry.getValue().getExtra();
      }
    }
    throw new Error("edge not found at index " + index);
  }
}
