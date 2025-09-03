package org.ton.ton4j.exporter.types;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the location of a file within an archive package. Contains all information needed to
 * read a file from an archive.
 */
@Data
@Builder
public class ArchiveFileLocation {

  /** Path to the archive package file (.pack) */
  private final String packagePath;

  /** Path to the archive index database (.index) */
  private final String indexPath;

  /** Offset within the package file where the file data starts */
  private final long offset;

  /** Package ID (extracted from filename, e.g., 100 for archive.00100.pack) */
  private final int packageId;

  /** File hash (hex string) used as key in the index database */
  private final String hash;

  /**
   * Checks if this location is valid for reading.
   *
   * @return true if all required fields are present and valid
   */
  public boolean isValid() {
    return packagePath != null
        && indexPath != null
        && hash != null
        && !hash.isEmpty()
        && offset >= 0
        && packageId >= 0;
  }

  /**
   * Creates an ArchiveFileLocation.
   *
   * @param packagePath Path to the package file
   * @param indexPath Path to the index database
   * @param hash File hash
   * @param packageId Package ID
   * @param offset Offset within package
   * @return ArchiveFileLocation instance
   */
  public static ArchiveFileLocation create(
      String packagePath, String indexPath, String hash, int packageId, long offset) {
    return ArchiveFileLocation.builder()
        .packagePath(packagePath)
        .indexPath(indexPath)
        .hash(hash)
        .packageId(packageId)
        .offset(offset)
        .build();
  }
}
