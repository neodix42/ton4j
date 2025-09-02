package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Reader for individual archive index databases (archive.XXXXX.index). Each archive package has a
 * corresponding RocksDB index that contains hash->offset mappings for files within that package.
 *
 * <p>Based on the C++ implementation in ArchiveFile class.
 */
@Slf4j
@Data
public class ArchiveIndexReader implements Closeable {

  private final String archiveIndexPath;
  private final String packagePath;
  private final int packageId;
  private RocksDbWrapper indexDb;

  /**
   * Creates a new ArchiveIndexReader for a specific archive index database.
   *
   * @param archiveIndexPath Path to the archive.XXXXX.index directory
   * @param packagePath Path to the corresponding archive.XXXXX.pack file
   * @param packageId The package ID (extracted from filename)
   * @throws IOException If the index database cannot be opened
   */
  public ArchiveIndexReader(String archiveIndexPath, String packagePath, int packageId)
      throws IOException {
    this.archiveIndexPath = archiveIndexPath;
    this.packagePath = packagePath;
    this.packageId = packageId;

    if (!Files.exists(Paths.get(archiveIndexPath))) {
      throw new IOException("Archive index database not found at: " + archiveIndexPath);
    }

    try {
      indexDb = new RocksDbWrapper(archiveIndexPath);
      log.debug("Opened archive index database: {}", archiveIndexPath);
    } catch (IOException e) {
      throw new IOException("Could not open archive index database: " + e.getMessage(), e);
    }
  }

  /**
   * Gets all hash->offset mappings from this archive index database. This follows the C++
   * implementation where each archive index contains file hashes as keys and offsets as values.
   *
   * @return Map of file hash to offset within the package file
   */
  public Map<String, Long> getAllHashOffsetMappings() {
    Map<String, Long> hashOffsetMap = new HashMap<>();

    AtomicInteger totalEntries = new AtomicInteger(0);
    AtomicInteger validMappings = new AtomicInteger(0);
    AtomicInteger parseErrors = new AtomicInteger(0);

    indexDb.forEach(
        (key, value) -> {
          try {
            totalEntries.incrementAndGet();

            String keyStr = new String(key);
            String valueStr = new String(value);

            // Skip special keys like "status"
            if ("status".equals(keyStr)) {
              return;
            }

            // Validate that key looks like a hash (hex string)
            if (!isValidHexString(keyStr) || keyStr.length() != 64) {
              return; // Skip non-hash keys
            }

            // Parse offset from value (stored as string in C++ implementation)
            try {
              long offset = Long.parseLong(valueStr);
              hashOffsetMap.put(keyStr, offset);
              validMappings.incrementAndGet();
            } catch (NumberFormatException e) {
              parseErrors.incrementAndGet();
              log.debug("Error parsing offset for hash {}: {}", keyStr, e.getMessage());
            }

          } catch (Exception e) {
            parseErrors.incrementAndGet();
            log.debug("Error processing archive index entry: {}", e.getMessage());
          }
        });

    log.debug(
        "Archive index {}: {} total entries, {} valid hash mappings, {} parse errors",
        packageId,
        totalEntries.get(),
        validMappings.get(),
        parseErrors.get());

    return hashOffsetMap;
  }

  /**
   * Gets the offset for a specific file hash.
   *
   * @param hash The file hash (hex string)
   * @return The offset within the package file, or null if not found
   */
  public Long getFileOffset(String hash) {
    try {
      byte[] valueBytes = indexDb.get(hash.getBytes());
      if (valueBytes == null) {
        return null;
      }

      String valueStr = new String(valueBytes);
      return Long.parseLong(valueStr);
    } catch (Exception e) {
      log.debug("Error getting offset for hash {}: {}", hash, e.getMessage());
      return null;
    }
  }

  /**
   * Gets the package status (current size) from the archive index. This corresponds to the "status"
   * key in the C++ implementation.
   *
   * @return The package size, or null if not found
   */
  public Long getPackageStatus() {
    try {
      byte[] valueBytes = indexDb.get("status".getBytes());
      if (valueBytes == null) {
        return null;
      }

      String valueStr = new String(valueBytes);
      return Long.parseLong(valueStr);
    } catch (Exception e) {
      log.debug("Error getting package status: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Gets the number of file entries in this archive index.
   *
   * @return Number of hash->offset mappings
   */
  public int getFileCount() {
    return getAllHashOffsetMappings().size();
  }

  /**
   * Checks if a string is a valid hexadecimal string.
   *
   * @param s The string to check
   * @return True if the string is a valid hexadecimal string, false otherwise
   */
  private static boolean isValidHexString(String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }
    return s.matches("^[0-9A-Fa-f]+$");
  }

  @Override
  public void close() throws IOException {
    if (indexDb != null) {
      indexDb.close();
      log.debug("Closed archive index database: {}", archiveIndexPath);
    }
  }
}
