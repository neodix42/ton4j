package org.ton.java.cell;

import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.bitstring.BitString;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class TonHashMapAug {

    public HashMap<Object, Pair<Cell, Cell>> elements;
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
    public TonHashMapAug(int keySize, int maxMembers) {
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
    public TonHashMapAug(int keySize) {
        elements = new LinkedHashMap<>();
        this.keySize = keySize;
        this.maxMembers = 10000;
    }

    public List<NodeAug> deserializeEdge(CellSlice edge, int keySize, final BitString key) {
        List<NodeAug> nodes = new ArrayList<>();
        BitString l = deserializeLabel(edge, keySize - key.toBitString().length());
        key.writeBitString(l);
        if (key.toBitString().length() == keySize) {
            Cell value = CellBuilder.beginCell().storeSlice(edge).endCell();
            //Cell extra = CellBuilder.beginCell().storeSlice(edge).endCell(); // todo how?
            Cell extra = CellBuilder.beginCell().storeUint(1, 16).endCell(); // todo how?
            List<NodeAug> newList = new ArrayList<>(nodes);
            newList.add(new NodeAug(key, value, extra));
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
    void deserialize(CellSlice c, Function<BitString, Object> keyParser, Function<Pair, Pair> valueParser) {
        List<NodeAug> nodes = deserializeEdge(c, keySize, new BitString(keySize));
        for (NodeAug node : nodes) {
            elements.put(keyParser.apply(node.key), valueParser.apply(Pair.of(node.value, node.extra)));
        }
    }

    /**
     * Read the keys in array and return binary tree in the form of nested array
     *
     * @param arr array which contains {key:Cell, value:Cell}
     * @return array either leaf or empty leaf or [left,right] fork
     */
    List<Object> splitTree(List<Object> arr) {
        List<Object> left = new ArrayList<>();
        List<Object> right = new ArrayList<>();

        for (Object a : arr) {
            BitString key = ((NodeAug) a).key;
            Cell value = ((NodeAug) a).value;
            Cell extra = ((NodeAug) a).extra;
            boolean lr = key.readBit();

            if (lr) {
                right.add(new NodeAug(key, value, extra));
            } else {
                left.add(new NodeAug(key, value, extra));
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

    void serialize_label(String label, int m, Cell builder) {
        int n = label.length();
        if (label.isEmpty()) {
            builder.bits.writeBit(false); //hml_short$0
            builder.bits.writeBit(false); //Unary 0
            return;
        }

        int sizeOfM = BigInteger.valueOf(m).bitLength();
        if (n < sizeOfM) {
            builder.bits.writeBit(false);  // hml_short
            for (int i = 0; i < n; i++) {
                builder.bits.writeBit(true); // Unary n
            }
            builder.bits.writeBit(false);  // Unary 0
            for (Character c : label.toCharArray()) {
                builder.bits.writeBit(c == '1');
            }
            return;
        }

        boolean isSame = ((label.equals("0".repeat(label.length()))) || label.equals("10".repeat(label.length())));
        if (isSame) {
            builder.bits.writeBit(true);
            builder.bits.writeBit(true); //hml_same
            builder.bits.writeBit(label.charAt(0) == '1');
            builder.bits.writeUint(label.length(), sizeOfM);
        } else {
            builder.bits.writeBit(true);
            builder.bits.writeBit(false); //hml_long
            builder.bits.writeUint(label.length(), sizeOfM);
            for (Character c : label.toCharArray()) {
                builder.bits.writeBit(c == '1');
            }
        }
    }

    void serialize_edge(List<Object> se, Cell builder) {
        if (se.size() == 0) {
            return;
        }
        if (se.size() == 3) { // contains leaf
            NodeAug node = (NodeAug) se.get(2);

//            BitString bs = node.key.readBits(node.key.writeCursor - node.key.readCursor); was
            BitString bs = node.key.readBits(node.key.getUsedBits());

            se.set(0, bs.toBitString());

            serialize_label((String) se.get(0), (Integer) se.get(1), builder);
            builder.writeCell(node.value);
            builder.writeCell(node.extra); // todo review
        } else { // contains fork
            serialize_label((String) se.get(0), (Integer) se.get(1), builder);
            Cell leftCell = new Cell();
            serialize_edge((List<Object>) se.get(2), leftCell);
            Cell rightCell = new Cell();
            serialize_edge((List<Object>) se.get(3), rightCell);
            builder.refs.add(leftCell);
            builder.refs.add(rightCell);
            //builder.writeCell(extra);
        }
    }

    public Cell serialize(Function<Object, BitString> keyParser, Function<Pair, Pair> valueParser) {
        List<Object> se = new ArrayList<>();
        for (Map.Entry<Object, Pair<Cell, Cell>> entry : elements.entrySet()) {
            BitString key = keyParser.apply(entry.getKey());
            Pair<Cell, Cell> value = valueParser.apply(entry.getValue());
            se.add(new NodeAug(key, value.getLeft(), value.getRight()));
        }

        if (se.isEmpty()) {
            throw new Error("TonHashMap does not support empty dict. Consider using TonHashMapE");
        }

        List<Object> s = flatten(splitTree(se), keySize);
        Cell b = new Cell();
        serialize_edge(s, b);

        return b;
    }

    public BitString deserializeLabel(CellSlice edge, int m) {
//        System.out.println("deserializeLabel " + edge.bits.writeCursor + " = " + edge.bits.toBitString());
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
}