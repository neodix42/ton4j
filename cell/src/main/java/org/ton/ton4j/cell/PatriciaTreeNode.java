package org.ton.ton4j.cell;

import java.io.Serializable;
import lombok.Data;

@Data
public class PatriciaTreeNode implements Serializable {
  String prefix;
  int maxPrefixLength;
  Node leafNode;
  PatriciaTreeNode left;
  PatriciaTreeNode right;

  public PatriciaTreeNode(
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
