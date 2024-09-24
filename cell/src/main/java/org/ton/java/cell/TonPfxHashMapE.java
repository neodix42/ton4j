package org.ton.java.cell;

import org.ton.java.bitstring.BitString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * TonPfxHashMapE with the fixed length keys.
 * With comparison to TonPfxHashMap, TonPfxHashMapE may be empty.
 * The first bit of it is a flag that indicates the emptiness.
 */
public class TonPfxHashMapE extends TonPfxHashMap {

    public TonPfxHashMapE(int keySize) {
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