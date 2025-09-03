package org.ton.ton4j.exporter.types;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Analysis results for CellDB metadata and cell data relationships.
 *
 * <p>This class provides insights into how metadata entries (containing block information) relate
 * to actual cell data entries in the CellDB database.
 */
@Data
@AllArgsConstructor
public class CellDataAnalysis {

  /** Total number of metadata entries found */
  private final int metadataCount;

  /** Total number of cell data entries found */
  private final int cellDataCount;

  /** Number of metadata entries that have corresponding cell data */
  private final int connectedEntries;

  /** Number of metadata entries missing their cell data */
  private final int missingCellData;

  /** Number of cell data entries not referenced by any metadata */
  private final int orphanedCellData;

  /**
   * Gets the percentage of metadata entries that have corresponding cell data.
   *
   * @return Percentage (0-100)
   */
  public double getConnectionPercentage() {
    if (metadataCount == 0) return 0.0;
    return (double) connectedEntries / metadataCount * 100.0;
  }

  /**
   * Gets the percentage of cell data entries that are orphaned (not referenced).
   *
   * @return Percentage (0-100)
   */
  public double getOrphanedPercentage() {
    if (cellDataCount == 0) return 0.0;
    return (double) orphanedCellData / cellDataCount * 100.0;
  }

  /**
   * Checks if the database appears to be in a healthy state. A healthy database should have most
   * metadata entries connected to cell data.
   *
   * @return True if connection percentage is above 90%
   */
  public boolean isHealthy() {
    return getConnectionPercentage() > 90.0;
  }

  @Override
  public String toString() {
    return String.format(
        "CellDataAnalysis{metadata=%d, cellData=%d, connected=%d (%.1f%%), missing=%d, orphaned=%d (%.1f%%), healthy=%s}",
        metadataCount,
        cellDataCount,
        connectedEntries,
        getConnectionPercentage(),
        missingCellData,
        orphanedCellData,
        getOrphanedPercentage(),
        isHealthy());
  }
}
