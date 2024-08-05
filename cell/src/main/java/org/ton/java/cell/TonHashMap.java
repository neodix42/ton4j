package org.ton.java.cell;

import org.ton.java.bitstring.BitString;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Ordinary Hashmap (Patricia Tree), with fixed length keys.
 */
public class TonHashMap {

    public HashMap<Object, Object> elements;
    int keySize;
    int maxMembers;

    /**
     * TonHashMap with the fixed length keys. TonHashMap cannot be empty.
     * If you plan to store empty Hashmap consider using TonHashMapE.
     * <p>
     * TonHashMap consists of two subsequent refs
     * Notice, all keys should be of the same size. If you have keys of different size - align them first.
     * Duplicates are not allowed.
     *
     * @param keySize    key size in bits
     * @param maxMembers max number of hashmap entries
     */
    public TonHashMap(int keySize, int maxMembers) {
        elements = new LinkedHashMap<>();
        this.keySize = keySize;
        this.maxMembers = maxMembers;
    }

    /**
     * HashMap with the fixed length keys.
     * TonHashMap cannot be empty. If you plan to store empty Hashmap consider using TonHashMapE.
     * <p>
     * Notice, all keys should be of the same size. If you have keys of different size - align them.
     * Duplicates are not allowed.
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

        PatriciaTreeNode leftNode = left.size() > 1
                ? splitTree(left)
                : left.isEmpty()
                    ? null
                    : new PatriciaTreeNode("", 0, left.get(0), null, null);
        PatriciaTreeNode rightNode = right.size() > 1
                ? splitTree(right)
                : right.isEmpty()
                    ? null
                    : new PatriciaTreeNode("", 0, right.get(0), null, null);

        return new PatriciaTreeNode("", keySize, null, leftNode, rightNode);
    }

    /**
     * Flatten binary tree (by cutting empty branches) if possible
     *
     * @param node tree node
     * @param m    maximal possible length of prefix
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

    /**
     * Serialize HashMap label
     * hml_short$0 {m:#} {n:#} len:(Unary ~n) {n <= m} s:(n * Bit) = HmLabel ~n m;
     * hml_long$10 {m:#} n:(#<= m) s:(n * Bit) = HmLabel ~n m;
     * hml_same$11 {m:#} v:Bit n:(#<= m) = HmLabel ~n m;
     *
     * @param label   String label string of zeroes and ones "010101"
     * @param m       int maximal possible length of the label
     * @param builder Cell to which label will be serialized
     */
    void serialize_label(String label, int m, CellBuilder builder) {
        int n = label.length();
        if (label.isEmpty()) {
            builder.storeBit(false); //hml_short$0
            builder.storeBit(false); //Unary 0
            return;
        }

        int sizeOfM = BigInteger.valueOf(m).bitLength();
        if (n < sizeOfM) {
            builder.storeBit(false);  // hml_short
            for (int i = 0; i < n; i++) {
                builder.storeBit(true); // Unary n
            }
            builder.storeBit(false);  // Unary 0
            for (Character c : label.toCharArray()) {
                builder.storeBit(c == '1');
            }
            return;
        }
        boolean isSame = (label.equals(Utils.repeat("0", label.length())) || label.equals(Utils.repeat("10", label.length())));

        if (isSame) {
            builder.storeBit(true);
            builder.storeBit(true); //hml_same
            builder.storeBit(label.charAt(0) == '1');
            builder.storeUint(label.length(), sizeOfM);
        } else {
            builder.storeBit(true);
            builder.storeBit(false); //hml_long
            builder.storeUint(label.length(), sizeOfM);
            for (Character c : label.toCharArray()) {
                builder.storeBit(c == '1');
            }
        }
    }

    /**
     * Serialize HashMap edge
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
     * @param m    length at most possible bits of n (key)
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
}