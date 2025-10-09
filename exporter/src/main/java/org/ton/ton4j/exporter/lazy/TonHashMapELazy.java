package org.ton.ton4j.exporter.lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;

/**
 * TonHashMapE with the fixed length keys. With comparison to TonHashMap, TonHashMapE may be empty.
 * The first bit of it is a flag that indicates the emptiness. Notice, all keys should be of the
 * same size. If you have keys of different size - align them. Duplicates are not allowed.
 */
public class TonHashMapELazy extends TonHashMapLazy {

  public TonHashMapELazy(int keySize) {
    super(keySize);
  }

  public Cell serialize(Function<Object, BitString> keyParser, Function<Object, Cell> valueParser) {
    List<Node> nodes = new ArrayList<>();
    for (Map.Entry<Object, Object> entry : elements.entrySet()) {
      BitString key = keyParser.apply(entry.getKey());
      Cell value = valueParser.apply(entry.getValue());
      nodes.add(new Node(key, value));
    }

    if (nodes.isEmpty()) {
      return null;
    } else {
      PatriciaTreeNode root = flatten(splitTree(nodes), keySize);
      CellBuilder b = CellBuilder.beginCell();
      serialize_edge(root, b);

      return b.endCell();
    }
  }
}
