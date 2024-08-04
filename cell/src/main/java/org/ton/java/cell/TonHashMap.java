package org.ton.java.cell;

import org.ton.java.bitstring.BitString;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
            List<Node> newList = new ArrayList<>(nodes);
            newList.add(new Node(key, value));
            return newList;
        }

        AtomicInteger i = new AtomicInteger();
        return edge.refs.stream().map(c -> {
            CellSlice forkEdge = CellSlice.beginParse(c);
            BitString forkKey = key.clone();
            forkKey.writeBit(i.get() != 0);
            i.getAndIncrement();
            return deserializeEdge(forkEdge, keySize, forkKey);
        }).reduce(new ArrayList<>(), (x, y) -> {
            x.addAll(y);
            return x;
        });
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
     * Read the keys in array and return binary tree in the form of nested array
     *
     * @param arr array which contains {key:Cell, value:Cell}
     * @return array either leaf or empty leaf or [left,right] fork
     */
    List<Object> splitTree(List<Object> arr) {
        if (arr.size() == 1) {
            // Return a structure that represents a leaf node
            return Arrays.asList(arr.get(0));
        }

        List<Object> left = new ArrayList<>();
        List<Object> right = new ArrayList<>();

        for (Object a : arr) {
            BitString key = ((Node) a).key;
            Cell value = ((Node) a).value;
            boolean lr = key.readBit();

            if (lr) {
                right.add(new Node(key, value));
            } else {
                left.add(new Node(key, value));
            }
        }

        if (left.size() > 1) {
            left = splitTree(left);
        }

        if (right.size() > 1) {
            right = splitTree(right);
        }

        if (left.isEmpty() && right.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(left, right));
    }

    /**
     * Flatten binary tree (by cutting empty branches) if possible:
     * [[], [[left,right]]] flatten to ["1", m, left, right]
     *
     * @param arr array which contains uncut tree
     * @param m   maximal possible length of prefix
     * @return {array} [prefix, maximal possible length of prefix, left branch tree, right branch tree]
     */
    List<Object> flatten(List<Object> arr, int m) {

        if (arr.size() == 0) {
            return arr;
        }

        if (arr.size() == 1 && arr.get(0) instanceof Node) {
            // This is a leaf node (single element case)
            return Arrays.asList("", m, arr.get(0));
        }

        if (!(arr.get(0) instanceof String)) {
            arr.addAll(0, Arrays.asList("", m));
        }

        if (arr.size() == 3) {
            return arr;
        }

        if (((ArrayList<?>) arr.get(2)).size() == 0) { // left empty
            return flatten(Arrays.asList(arr.get(0) + "1", arr.get(1), ((ArrayList<?>) arr.get(3)).get(0), ((ArrayList<?>) arr.get(3)).get(1)), m);
        } else if (((ArrayList<?>) arr.get(3)).size() == 0) { // right empty
            return flatten(Arrays.asList(arr.get(0) + "0", arr.get(1), ((ArrayList<?>) arr.get(2)).get(0), ((ArrayList<?>) arr.get(2)).get(1)), m);
        } else {
            return new ArrayList<>(Arrays.asList(
                    arr.get(0),
                    arr.get(1),
                    flatten((ArrayList) arr.get(2), m - ((String) arr.get(0)).length() - 1),
                    flatten((ArrayList) arr.get(3), m - ((String) arr.get(0)).length() - 1)
            ));
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
     * @param se      List<Object> which contains [label as "0" and "1" string, maximal possible size of label, leaf or left fork, right fork]
     * @param builder Cell to which edge will be serialized
     */
    void serialize_edge(List<Object> se, CellBuilder builder) {
        if (se.size() == 0) {
            return;
        }
        if (se.size() == 3) { // contains leaf
            Node node = (Node) se.get(2);
            BitString bs = node.key.readBits(node.key.getUsedBits());
            se.set(0, bs.toBitString());
            serialize_label((String) se.get(0), (Integer) se.get(1), builder);
            builder.storeCell(node.value);
        } else { // contains fork
            serialize_label((String) se.get(0), (Integer) se.get(1), builder);
            CellBuilder leftCell = CellBuilder.beginCell();
            serialize_edge((List<Object>) se.get(2), leftCell);
            CellBuilder rightCell = CellBuilder.beginCell();
            serialize_edge((List<Object>) se.get(3), rightCell);
            builder.storeRef(leftCell.endCell());
            builder.storeRef(rightCell.endCell());
        }
    }

    public Cell serialize(Function<Object, BitString> keyParser, Function<Object, Cell> valueParser) {
        List<Object> se = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : elements.entrySet()) {
            BitString key = keyParser.apply(entry.getKey());
            Cell value = valueParser.apply(entry.getValue());
            se.add(new Node(key, value));
        }

        if (se.isEmpty()) {
            throw new Error("TonHashMap does not support empty dict. Consider using TonHashMapE");
        }

        List<Object> s = flatten(splitTree(se), keySize);
        CellBuilder b = CellBuilder.beginCell();
        serialize_edge(s, b);

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