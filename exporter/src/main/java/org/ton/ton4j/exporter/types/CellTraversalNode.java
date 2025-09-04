package org.ton.ton4j.exporter.types;

/** Helper class for cell tree traversal. */
public class CellTraversalNode {
  public final String hash;
  public final int depth;

  public CellTraversalNode(String hash, int depth) {
    this.hash = hash;
    this.depth = depth;
  }
}
