package org.ton.java.cell;

import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.bitstring.BitString;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TonHashMapAug {

    public HashMap<Object, Pair<Object, Object>> elements; // Pair<Value,Extra>
    int keySize;
    int maxMembers;

    public TonHashMapAug(int keySize, int maxMembers) {
        elements = new LinkedHashMap<>();
        this.keySize = keySize;
        this.maxMembers = maxMembers;
    }

    public TonHashMapAug(int keySize) {
        elements = new LinkedHashMap<>();
        this.keySize = keySize;
        this.maxMembers = 10000;
    }

    public List<Node> deserializeEdge(CellSlice edge, int keySize, final BitString key) {
        if (edge.type != CellType.ORDINARY) {
            return new ArrayList<>();
        }
        List<Node> nodes = new ArrayList<>();
        BitString l = deserializeLabel(edge, keySize - key.toBitString().length());
        key.writeBitString(l);
        if (key.toBitString().length() == keySize) {
            Cell valueAndExtra = CellBuilder.beginCell().storeSlice(edge).endCell();
            List<Node> newList = new ArrayList<>(nodes);
            newList.add(new Node(key, valueAndExtra)); // fork-extra does not exist in edge
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
     * Loads HashMapAug and parses keys, values and extras
     */
    void deserialize(CellSlice c,
                     Function<BitString, Object> keyParser,
                     Function<CellSlice, Object> valueParser,
                     Function<CellSlice, Object> extraParser) {
        List<Node> nodes = deserializeEdge(c, keySize, new BitString(keySize));
        for (Node node : nodes) {
            CellSlice valueAndExtra = CellSlice.beginParse(node.value);
            Object extra = extraParser.apply(valueAndExtra);
            Object value = valueParser.apply(valueAndExtra);
            elements.put(keyParser.apply(node.key), Pair.of(value, extra));
        }
    }

    /**
     * Read the keys in array and return binary tree in the form of nested array
     *
     * @param arr array which contains {key:Cell, value:Cell, extra:Cell}
     * @return array either leaf or empty leaf or [left,right] fork
     */
    List<Object> splitTree(List<Object> arr) {
        List<Object> left = new ArrayList<>();
        List<Object> right = new ArrayList<>();

        for (Object a : arr) {
            BitString key = ((Node) a).key;
            Cell valueAndExtra = ((Node) a).value;
//            Cell extra = ((NodeAug) a).extra;
            boolean lr = key.readBit();

            if (lr) {
                right.add(new Node(key, valueAndExtra));
            } else {
                left.add(new Node(key, valueAndExtra));
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

    void serialize_edge(List<Object> se, Cell builder, BiFunction<Object, Object, Object> forkExtra) {
        if (se.size() == 0) {
            return;
        }
        if (se.size() == 3) { // contains leaf
            Node node = (Node) se.get(2);

            BitString bs = node.key.readBits(node.key.getUsedBits());

            se.set(0, bs.toBitString());

            serialize_label((String) se.get(0), (Integer) se.get(1), builder);
            builder.writeCell(node.value);
        } else { // contains fork
            serialize_label((String) se.get(0), (Integer) se.get(1), builder);
            Cell leftCell = new Cell();
            serialize_edge((List<Object>) se.get(2), leftCell, forkExtra);
            Cell rightCell = new Cell();
            serialize_edge((List<Object>) se.get(3), rightCell, forkExtra);
            builder.writeCell((Cell) forkExtra.apply(leftCell, rightCell));
            builder.refs.add(leftCell);
            builder.refs.add(rightCell);
        }
    }

    /**
     * Serializes edges and puts values into fork-nodes according to forkExtra function logic
     *
     * @param keyParser   - used on key
     * @param valueParser - used on every leaf
     * @param extraParser - used on every leaf
     * @param forkExtra   - used only in fork-node.
     * @return Cell
     */
    public Cell serialize(Function<Object, BitString> keyParser,
                          Function<Object, Object> valueParser,
                          Function<Object, Object> extraParser,
                          BiFunction<Object, Object, Object> forkExtra) {
        List<Object> se = new ArrayList<>();
        for (Map.Entry<Object, Pair<Object, Object>> entry : elements.entrySet()) {
            BitString key = keyParser.apply(entry.getKey());
            Cell value = (Cell) valueParser.apply(entry.getValue().getLeft());
            Cell extra = (Cell) extraParser.apply(entry.getValue().getRight());
            Cell both = CellBuilder.beginCell()
                    .storeSlice(CellSlice.beginParse(extra))
                    .storeSlice(CellSlice.beginParse(value))
                    .endCell();

            se.add(new Node(key, both));
        }

        if (se.isEmpty()) {
            throw new Error("TonHashMapAug does not support empty dict. Consider using TonHashMapAugE");
        }

        List<Object> s = flatten(splitTree(se), keySize);
        Cell b = new Cell();
        serialize_edge(s, b, forkExtra);

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


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (Map.Entry<Object, Pair<Object, Object>> entry : elements.entrySet()) {
            String s = String.format("[%s,(%s,%s)],", entry.getKey(), entry.getValue().getLeft(), entry.getValue().getRight());
            sb.append(s);
        }
        if (!elements.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        sb.append(")");
        return sb.toString();
    }
}