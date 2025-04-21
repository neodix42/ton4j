package org.ton.java.cell;

import java.io.Serializable;

public class PatriciaTreeNode implements Serializable {
  String prefix;
  int maxPrefixLength;
  Node leafNode;
  PatriciaTreeNode left;
  PatriciaTreeNode right;

  PatriciaTreeNode(
      String prefix,
      int maxPrefixLength,
      Node leafNode,
      PatriciaTreeNode left,
      PatriciaTreeNode right) {
    this.prefix = prefix;
    this.maxPrefixLength = maxPrefixLength;
    this.leafNode = leafNode;
    this.left = left;
    this.right = right;
  }
}
