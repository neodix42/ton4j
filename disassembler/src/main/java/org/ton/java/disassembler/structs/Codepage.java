package org.ton.java.disassembler.structs;

import java.util.List;
import java.util.function.BiFunction;
import org.ton.java.cell.CellSlice;

public class Codepage {
    private final Trie<Object> trie = new Trie<>();

    public void insertHex(String hex, int len, BiFunction<CellSlice, Integer, String> op) {
        StringBuilder prefix = new StringBuilder(Integer.toBinaryString(Integer.parseInt(hex, 16)));
        while (prefix.length() < len) {
            prefix.insert(0, "0");
        }
        if (prefix.length() > len) {
            prefix = new StringBuilder(prefix.substring(0, len));
        }
        trie.insert(prefix.toString(), op);
    }

    public void insertBin(String bin, BiFunction<CellSlice, Integer, String> op) {
        trie.insert(bin, op);
    }

    public Object getOp(String bitPrefix) {
        return trie.getValue(bitPrefix);
    }

    public List<String> find(String prefix, int maxOccurrences) {
        return trie.find(prefix, maxOccurrences);
    }
}