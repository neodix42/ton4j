package org.ton.ton4j.exporter.lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.utils.Utils;

/**
 * A special case of a dictionary with variable-length keys is that of a prefix code, where the keys
 * cannot be prefixes of each other. Values in such dictionaries may occur only in the leaves of a
 * Patricia tree.
 */
public class TonPfxHashMapLazy extends TonHashMapLazy {

  public TonPfxHashMapLazy(int keySize) {
    super(keySize);
  }

  public List<Node> deserializeEdge(CellSliceLazy edge, int keySize, final BitString key) {
    List<Node> nodes = new ArrayList<>();
    BitString l = deserializeLabel(edge, keySize - key.toBitString().length());
    key.writeBitString(l);
    boolean pfx = edge.loadBit(); // pfx feature
    if (!pfx) {
      Cell value = CellBuilder.beginCell().storeSliceLazy(edge.bits, edge.getHashes()).endCell();
      nodes.add(new Node(key, value));
      return nodes;
    }

    //    for (int j = 0; j < edge.refs.size(); j++) {
    for (int j = 0; j < edge.hashes.length / 32; j++) {
      byte[] hash = Utils.slice(edge.hashes, j, ((j == 0) ? 1 : j + 1) * 32);
      Cell refCell = edge.getRefByHash(hash);
      CellSliceLazy forkEdge = CellSliceLazy.beginParse(edge.cellDbReader, refCell);
      //      CellSliceLazy forkEdge = CellSliceLazy.beginParse(edge.refs.get(j));
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
   * Serialize PfxHashMap edge hm_edge#_ {n:#} {X:Type} {l:#} {m:#} label:(HmLabel ~l n) {n = (~m) +
   * l} node:(HashmapNode m X) = Hashmap n X;
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
      builder.storeBit(false); // pfx feature
      builder.storeCell(node.getLeafNode().getValue());
    } else { // contains fork
      serialize_label(node.getPrefix(), node.getMaxPrefixLength(), builder);
      builder.storeBit(true); // pfx feature
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
      throw new Error("TonPfxHashMap does not support empty dict. Consider using TonPfxHashMapE");
    }

    PatriciaTreeNode root = flatten(splitTree(nodes), keySize);
    CellBuilder b = CellBuilder.beginCell();
    serialize_edge(root, b);

    return b.endCell();
  }
}
