package org.ton.ton4j.cell;

import org.ton.ton4j.bitstring.BitString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A special case of a dictionary with
 * variable-length keys is that of a prefix code, where the keys cannot be prefixes
 * of each other. Values in such dictionaries may occur only in the leaves of a
 * Patricia tree.
 */
public class TonPfxHashMap extends TonHashMap {

    public TonPfxHashMap(int keySize) {
        super(keySize);
    }

    public List<Node> deserializeEdge(CellSlice edge, int keySize, final BitString key) {
        List<Node> nodes = new ArrayList<>();
        BitString l = deserializeLabel(edge, keySize - key.toBitString().length());
        key.writeBitString(l);
        boolean pfx = edge.loadBit(); // pfx feature
        if (!pfx) {
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


    /**
     * Loads HashMap and parses keys and values
     * HashMap X Y;
     */
    void deserialize(CellSlice c, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
        List<Node> nodes = deserializeEdge(c, keySize, new BitString(keySize));
        for (Node node : nodes) {
            elements.put(keyParser.apply(node.key), valueParser.apply(node.value));
        }
    }

    /**
     * Serialize PfxHashMap edge
     * hm_edge#_ {n:#} {X:Type} {l:#} {m:#} label:(HmLabel ~l n)
     * {n = (~m) + l} node:(HashmapNode m X) = Hashmap n X;
     *
     * @param node    tree node which contains [label as "0" and "1" string, maximal possible size of label, leaf or left fork, right fork]
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
            builder.storeBit(false); //pfx feature
            builder.storeCell(node.leafNode.value);
        } else { // contains fork
            serialize_label(node.prefix, node.maxPrefixLength, builder);
            builder.storeBit(true); //pfx feature
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
            throw new Error("TonPfxHashMap does not support empty dict. Consider using TonPfxHashMapE");
        }

        PatriciaTreeNode root = flatten(splitTree(nodes), keySize);
        CellBuilder b = CellBuilder.beginCell();
        serialize_edge(root, b);

        return b.endCell();
    }
}