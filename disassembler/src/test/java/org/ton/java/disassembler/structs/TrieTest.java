package org.ton.java.disassembler.structs;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TrieTest {

  @Test
  public void shouldInsertAndFindTest() {
    Trie<String> trie = new Trie<>();
    String key1 = "Eric", val1 = "Smith";
    String key2 = "Ermo", val2 = "Frost";

    trie.insert(key1, val1);
    trie.insert(key2, val2);
    boolean result = trie.contains(key1);
    assertThat(result).isTrue();
    assertThat(Arrays.asList(key2, key1))
        .isEqualTo(trie.find("Er")); // supports lower case and upper case separately
    assertThat(Collections.emptyList()).isEqualTo(trie.find("er")); // empty list

    String key3 = "John", val3 = "Doe";
    trie.insert(key3, val3);
    assertThat(Collections.singletonList(key3)).isEqualTo(trie.find(key3));

    String resultValue = trie.getValue(key3);
    assertThat(resultValue).isNotNull();
    assertThat(val3).isEqualTo(resultValue);

    assertThat(trie.getValue("lo")).isNull();
    assertThat(trie.getValue("fA")).isNull();
  }
}
