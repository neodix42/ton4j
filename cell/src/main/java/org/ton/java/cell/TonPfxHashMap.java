package org.ton.java.cell;

import org.ton.java.bitstring.BitString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class TonPfxHashMap extends TonHashMap {

    /**
     * HashMap with the fixed length keys.
     * With comparison to TonHashMap, TonHashMapE may be empty.
     * The first bit of it is a flag that indicates the emptiness.
     * Notice, all keys should be of the same size. If you have keys of different size - align them.
     * Duplicates are not allowed.
     *
     * @param keySize key size in bits
     */
    public TonPfxHashMap(int keySize) {
        super(keySize);
    }

    public List<Node> deserializeEdge(CellSlice edge, int keySize, final BitString key) {
        List<Node> nodes = new ArrayList<>();
        BitString l = deserializeLabel(edge, keySize - key.toBitString().length());
        key.writeBitString(l);
        boolean pfx = edge.loadBit(); // pfx feature
        if (pfx == false) {
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
     * Serialize PfxHashMap edge
     * hm_edge#_ {n:#} {X:Type} {l:#} {m:#} label:(HmLabel ~l n)
     * {n = (~m) + l} node:(HashmapNode m X) = Hashmap n X;
     *
     * @param se      List<Object> which contains [label as "0" and "1" string, maximal possible size of label, leaf or left fork, right fork]
     * @param builder Cell to which edge will be serialized
     */
    void serialize_edge(List<Object> se, Cell builder) {
        if (se.size() == 0) {
            return;
        }

        if (se.size() == 3) { // contains leaf
            Node node = (Node) se.get(2);

//            BitString bs = node.key.readBits(node.key.writeCursor - node.key.readCursor); was
            BitString bs = node.key.readBits(node.key.length - node.key.getUsedBits()); // todo

            se.set(0, bs.toBitString());

            serialize_label((String) se.get(0), (Integer) se.get(1), builder);
            builder.bits.writeBit(false); //pfx feature
            builder.writeCell(node.value);
        } else { // contains fork
            serialize_label((String) se.get(0), (Integer) se.get(1), builder);
            builder.bits.writeBit(true); //pfx feature
            Cell leftCell = new Cell();
            serialize_edge((List<Object>) se.get(2), leftCell);
            Cell rightCell = new Cell();
            serialize_edge((List<Object>) se.get(3), rightCell);
            builder.refs.add(leftCell);
            builder.refs.add(rightCell);
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
            throw new Error("TonPfxHashMap does not support empty dict. Consider using TonPfxHashMapE");
        }

        List<Object> s = flatten(splitTree(se), keySize);
        Cell b = new Cell();
        serialize_edge(s, b);

        return b;
    }
}