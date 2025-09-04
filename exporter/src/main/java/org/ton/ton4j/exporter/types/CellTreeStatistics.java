package org.ton.ton4j.exporter.types;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/** Statistics about a cell tree. */
public class CellTreeStatistics {
  // Getters
  @Getter private final String rootHash;
  @Getter private final int totalCells;
  private final Map<CellType, Integer> typeCounts;
  @Getter private final int totalSize;
  @Getter private final int maxDepth;

  public CellTreeStatistics(
      String rootHash,
      int totalCells,
      Map<CellType, Integer> typeCounts,
      int totalSize,
      int maxDepth) {
    this.rootHash = rootHash;
    this.totalCells = totalCells;
    this.typeCounts = new HashMap<>(typeCounts);
    this.totalSize = totalSize;
    this.maxDepth = maxDepth;
  }

  public Map<CellType, Integer> getTypeCounts() {
    return new HashMap<>(typeCounts);
  }

  @Override
  public String toString() {
    return String.format(
        "CellTreeStatistics{root=%s, cells=%d, size=%d, types=%s}",
        rootHash, totalCells, totalSize, typeCounts);
  }
}
