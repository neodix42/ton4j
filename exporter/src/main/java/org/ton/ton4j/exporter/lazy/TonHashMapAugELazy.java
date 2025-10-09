package org.ton.ton4j.exporter.lazy;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;

public class TonHashMapAugELazy extends TonHashMapAugLazy {

  /**
   * TonHashMapAugE with the fixed length keys. With comparison to TonHashMapAug, TonHashMapAugE may
   * be empty. The first bit of it is a flag that indicates the emptiness. Notice, all keys should
   * be of the same size. If you have keys of different size - align them. Duplicates are not
   * allowed.
   *
   * @param keySize key size in bits
   */
  public TonHashMapAugELazy(int keySize) {
    super(keySize);
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
      return CellBuilder.beginCell().storeBit(false).endCell();
    } else {
      PatriciaTreeNode root = flatten(splitTree(nodes), keySize);
      CellBuilder b = CellBuilder.beginCell();
      serialize_edge(root, b, forkExtra);

      return CellBuilder.beginCell().storeBit(true).storeRef(b.endCell()).endCell();
    }
  }
}
