package org.ton.java.cell;

import org.ton.java.bitstring.BitString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TonPfxHashMapE extends TonPfxHashMap {

    /**
     * TonPfxHashMapE with the fixed length keys.
     * With comparison to TonPfxHashMap, TonPfxHashMapE may be empty.
     * The first bit of it is a flag that indicates the emptiness.
     * Notice, all keys should be of the same size. If you have keys of different size - align them.
     * Duplicates are not allowed.
     *
     * @param keySize key size in bits
     */
    public TonPfxHashMapE(int keySize) {
        super(keySize);
    }

    public Cell serialize(Function<Object, BitString> keyParser, Function<Object, Cell> valueParser) {
        List<Object> se = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : elements.entrySet()) {
            BitString key = keyParser.apply(entry.getKey());
            Cell value = valueParser.apply(entry.getValue());
            se.add(new Node(key, value));
        }

        if (se.isEmpty()) {
            return CellBuilder.beginCell().storeBit(false).endCell();
        } else {
            List<Object> s = flatten(splitTree(se), keySize);
            Cell b = new Cell();
            serialize_edge(s, b);

            return CellBuilder.beginCell()
                    .storeBit(true)
                    .storeRef(b)
                    .endCell();
        }
    }
}