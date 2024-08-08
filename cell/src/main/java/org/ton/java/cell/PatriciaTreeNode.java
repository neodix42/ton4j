package org.ton.java.cell;

public class PatriciaTreeNode {
    String prefix;
    int maxPrefixLength;
    Node leafNode;
    PatriciaTreeNode left;
    PatriciaTreeNode right;

    PatriciaTreeNode(String prefix, int maxPrefixLength, Node leafNode, PatriciaTreeNode left, PatriciaTreeNode right) {
        this.prefix = prefix;
        this.maxPrefixLength = maxPrefixLength;
        this.leafNode = leafNode;
        this.left = left;
        this.right = right;
    }
}