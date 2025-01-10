package org.ton.java.disassembler.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class TrieTest {

    @Test
    void shouldInsertAndFind() {
        Trie<String> trie = new Trie<>();
        String key1 = "Eric", val1 = "Smith";
        String key2 = "Ermo", val2 = "Frost";

        trie.insert(key1, val1);
        trie.insert(key2, val2);
        boolean result = trie.contains(key1);
        assertTrue(result);
        assertEquals(Arrays.asList(key2, key1), trie.find("Er")); // supports lower case and upper case separately
        assertEquals(Arrays.asList(), trie.find("er")); // empty list

        String key3 = "John", val3 = "Doe";
        trie.insert(key3, val3);
        assertEquals(Arrays.asList(key3), trie.find(key3));

        String resultValue = trie.getValue(key3);
        assertNotNull(resultValue);
        assertEquals(val3, resultValue);

        assertNull(trie.getValue("lo"));
        assertNull(trie.getValue("fA"));
    }
}
