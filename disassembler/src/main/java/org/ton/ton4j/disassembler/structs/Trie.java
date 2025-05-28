package org.ton.ton4j.disassembler.structs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TrieNode<T> {
    public final Character key;
    public TrieNode<T> parent;
    public Map<Character, TrieNode<T>> children;
    public boolean end;
    public T value;

    public TrieNode(Character key, T value) {
        this.parent = null;
        this.children = new HashMap<>();
        this.end = false;
        this.key = key;
        this.value = value;
    }

    public String getWord() {
        StringBuilder output = new StringBuilder();
        TrieNode<T> node = this;
        while (node != null) {
            if (node.key != null) {
                output.insert(0, node.key);
            }
            node = node.parent;
        }
        return output.toString();
    }
}

public class Trie<T> {
    private final TrieNode<T> root;

    public Trie() {
        this.root = new TrieNode<>(null, null);
    }

    public void insert(String word, T value) {
        TrieNode<T> node = this.root;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (node.end) {
                System.out.println(node.getWord() + " " + node.value);
                throw new RuntimeException("Word cannot start with already used prefix");
            }
            if (!node.children.containsKey(ch)) {
                node.children.put(ch, new TrieNode<>(ch, null));
                node.children.get(ch).parent = node;
            }

            node = node.children.get(ch);
            if (i == word.length() - 1) {
                if (!node.children.isEmpty()) {
                    System.out.println(node.getWord() + " " + find(node.getWord()));
                    throw new RuntimeException("Word cannot start with already used prefix");
                }
                node.end = true;
                node.value = value;
            }
        }
    }

    public boolean contains(String word) {
        TrieNode<T> node = this.root;
        for (char ch : word.toCharArray()) {
            if (node.children.containsKey(ch)) {
                node = node.children.get(ch);
            } else {
                return false;
            }
        }
        return node.end;
    }

    public List<String> find(String prefix) {
        return find(prefix, -1);
    }

    public List<String> find(String prefix, int maxOccurrences) {
        TrieNode<T> node = this.root;
        List<String> output = new ArrayList<>();
        for (char ch : prefix.toCharArray()) {
            if (node.children.containsKey(ch)) {
                node = node.children.get(ch);
            } else {
                return output;
            }
        }
        this.findAllWords(node, output, maxOccurrences);
        return output;
    }

    private void findAllWords(TrieNode<T> node, List<String> arr, int maxOccurrences) {
        if (node.end) {
            arr.add(0, node.getWord());
        }
        if (maxOccurrences != -1 && arr.size() >= maxOccurrences) {
            return;
        }
        for (TrieNode<T> child : node.children.values()) {
            this.findAllWords(child, arr, maxOccurrences);
        }
    }

    public T getValue(String key) {
        TrieNode<T> node = this.root;
        for (char ch : key.toCharArray()) {
            if (node.children.containsKey(ch)) {
                node = node.children.get(ch);
            } else {
                return null;
            }
        }
        if (!node.end) {
            return null;
        }
        return node.value;
    }
}