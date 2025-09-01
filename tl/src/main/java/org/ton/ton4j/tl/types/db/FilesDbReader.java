package org.ton.ton4j.tl.types.db;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.tl.types.db.files.index.IndexValue;
import org.ton.ton4j.tl.types.db.files.key.PackageKey;
import org.ton.ton4j.tl.types.db.files.package_.PackageValue;

/**
 * Specialized reader for TON Files database global index. This reader focuses specifically on the
 * Files database structure: - Global index at db/files/globalindex (RocksDB) - Package files at
 * db/files/packages/*.pack
 *
 * <p>Based on the original TON C++ implementation, the Files database uses: 1. Main index entry:
 * key = db.files.index.key (empty), value = db.files.index.value (package lists) 2. Package
 * metadata entries: key = db.files.package.key, value = db.files.package.value 3. File hash
 * entries: key = file hash (raw bytes), value = location data (package_id, offset, size)
 */
@Slf4j
@Data
public class FilesDbReader implements Closeable {

  private final String dbPath;
  private RocksDbWrapper globalIndexDb;
  private final Map<String, PackageReader> packageReaders = new HashMap<>();

  // Cache for package metadata
  private final Map<Long, PackageValue> packageMetadata = new HashMap<>();
  private IndexValue mainIndexIndexValue;

  /**
   * Creates a new FilesDbReader.
   *
   * @param dbPath Path to the database root directory (should contain files/globalindex)
   * @throws IOException If an I/O error occurs
   */
  public FilesDbReader(String dbPath) throws IOException {
    this.dbPath = dbPath;
    initializeFilesDatabase();

  }

  /** Initializes the Files database global index. */
  private void initializeFilesDatabase() throws IOException {
    Path filesPath = Paths.get(dbPath, "files");
    Path globalIndexPath = filesPath.resolve("globalindex");

    if (!Files.exists(globalIndexPath)) {
      throw new IOException("Files database global index not found at: " + globalIndexPath);
    }

    try {
      globalIndexDb = new RocksDbWrapper(globalIndexPath.toString());
      log.info("Initialized Files database global index: {}", globalIndexPath);
    } catch (IOException e) {
      throw new IOException(
          "Could not initialize Files database global index: " + e.getMessage(), e);
    }
  }


  /** Loads metadata for a specific package. */
  private void loadPackageMetadata(int packageId, boolean isKey, boolean isTemp) {
    try {
      PackageKey packageKey =
          PackageKey.builder().packageId(packageId).key(isKey).temp(isTemp).build();

      byte[] packageKeyBytes = packageKey.serialize();
      byte[] packageValueBytes = globalIndexDb.get(packageKeyBytes);

      if (packageValueBytes != null) {
        ByteBuffer buffer = ByteBuffer.wrap(packageValueBytes);
        PackageValue packageValue = PackageValue.deserialize(buffer);

        packageMetadata.put((long) packageId, packageValue);

        log.debug(
            "Loaded package {} metadata: key={}, temp={}, deleted={}, firstblocks={}",
            packageId,
            packageValue.isKey(),
            packageValue.isTemp(),
            packageValue.isDeleted(),
            packageValue.getFirstblocks().size());
      }
    } catch (Exception e) {
      log.debug("Error loading package {} metadata: {}", packageId, e.getMessage());
    }
  }

  /**
   * Gets all BlockLocations from the Files database global index. This reads file hash entries that
   * map hash → (package_id, offset, size).
   *
   * @return Map of file hash to BlockLocation
   */
  public Map<String, BlockLocation> getAllBlockLocations() {
    Map<String, BlockLocation> blockLocations = new HashMap<>();

    log.info("Reading BlockLocations from Files database global index...");

    AtomicInteger totalEntries = new AtomicInteger(0);
    AtomicInteger validLocations = new AtomicInteger(0);
    AtomicInteger parseErrors = new AtomicInteger(0);

    globalIndexDb.forEach(
        (key, value) -> {
          try {
            totalEntries.incrementAndGet();

            String hash = new String(key);

            // Skip TL-serialized keys (they are metadata, not file hash entries)
            if (!isValidHexString(hash) || hash.length() != 64) {
              return; // Skip non-hex keys or wrong length
            }

            // Parse the value as direct location data (package_id, offset, size)
            BlockLocation location = parseFileHashEntry(hash, value);
            if (location != null && location.isValid()) {
              blockLocations.put(hash, location);
              validLocations.incrementAndGet();
            } else {
              parseErrors.incrementAndGet();
            }
          } catch (Exception e) {
            parseErrors.incrementAndGet();
            log.debug("Error processing global index entry: {}", e.getMessage());
          }
        });

    log.info(
        "Files database parsing: {} total entries, {} valid BlockLocations, {} parse errors",
        totalEntries.get(),
        validLocations.get(),
        parseErrors.get());

    return blockLocations;
  }

  /**
   * Parses a file hash entry from the global index. Based on the original C++ implementation, file
   * hash entries contain: Format: [package_id: 8 bytes][offset: 8 bytes][size: 8 bytes]
   * (little-endian)
   *
   * @param hash The file hash
   * @param value The raw value from RocksDB
   * @return BlockLocation or null if parsing fails
   */
  private BlockLocation parseFileHashEntry(String hash, byte[] value) {
    if (value == null || value.length < 16) {
      return null; // Need at least package_id + offset
    }

    try {
      ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

      // Extract package_id (8 bytes)
      long packageId = buffer.getLong();

      // Extract offset (8 bytes)
      long offset = buffer.getLong();

      // Extract size (8 bytes if available, otherwise default)
      long size = 1024; // Default size
      if (value.length >= 24) {
        size = buffer.getLong();
      } else if (value.length >= 20) {
        size = buffer.getInt() & 0xFFFFFFFFL; // 4-byte unsigned size
      }

      // Validate values
      if (packageId >= 0
          && packageId < 1_000_000_000L
          && // Reasonable package ID range
          offset >= 0
          && offset < 10_000_000_000L
          && // Reasonable offset range
          size > 0
          && size < 100_000_000) { // Reasonable size range
        return BlockLocation.create(hash, packageId, offset, size);
      }

      return null;
    } catch (Exception e) {
      log.debug("Error parsing file hash entry for {}: {}", hash, e.getMessage());
      return null;
    }
  }

  /**
   * Reads a block by its hash using the optimized Files database approach.
   *
   * @param hash The block hash
   * @return The block data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public byte[] readBlock(String hash) throws IOException {
    log.debug("Reading block from Files database: {}", hash);

    // Get the location from global index
    byte[] locationBytes = globalIndexDb.get(hash.getBytes());
    if (locationBytes == null) {
      log.debug("Block {} not found in Files database global index", hash);
      return null;
    }

    // Parse location data
    BlockLocation location = parseFileHashEntry(hash, locationBytes);
    if (location == null || !location.isValid()) {
      log.warn("Invalid location data for block {}", hash);
      return null;
    }

    // Read from package file
    return readBlockFromPackage(location);
  }

  /**
   * Reads a block from a package file using BlockLocation.
   *
   * @param location The BlockLocation containing package_id, offset, and size
   * @return The block data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public byte[] readBlockFromPackage(BlockLocation location) throws IOException {
    if (location == null || !location.isValid()) {
      return null;
    }

    // Get package file path
    String packagePath = getPackageFilePath(location.getPackageId());
    if (packagePath == null) {
      log.warn("Package file not found for package ID: {}", location.getPackageId());
      return null;
    }

    // Get package reader
    PackageReader packageReader = getPackageReader(packagePath);

    // Read entry at offset
    PackageReader.PackageEntry entry = packageReader.getEntryAt(location.getOffset().longValue());
    if (entry == null) {
      log.warn(
          "No entry found at offset {} in package {}",
          location.getOffset(),
          location.getPackageId());
      return null;
    }

    return entry.getData();
  }

  /**
   * Gets the file path for a package by its ID.
   *
   * @param packageId The package ID
   * @return The file path or null if not found
   */
  public String getPackageFilePath(long packageId) {
    try {
      String packageFileName = String.format("%010d.pack", packageId);
      Path filesPackagesPath = Paths.get(dbPath, "files", "packages");
      Path packagePath = filesPackagesPath.resolve(packageFileName);

      if (Files.exists(packagePath)) {
        return packagePath.toString();
      }
    } catch (Exception e) {
      log.debug("Error getting package file path for ID {}: {}", packageId, e.getMessage());
    }
    return null;
  }

  /**
   * Gets a package reader for a specific package file.
   *
   * @param packagePath Path to the package file
   * @return The PackageReader
   * @throws IOException If an I/O error occurs
   */
  private PackageReader getPackageReader(String packagePath) throws IOException {
    if (!packageReaders.containsKey(packagePath)) {
      packageReaders.put(packagePath, new PackageReader(packagePath));
    }
    return packageReaders.get(packagePath);
  }

  /**
   * Gets the main index value containing package lists.
   *
   * @return The main index value
   */
  public IndexValue getMainIndexIndexValue() {
    return mainIndexIndexValue;
  }

  /**
   * Gets package metadata for a specific package ID.
   *
   * @param packageId The package ID
   * @return Package metadata or null if not found
   */
  public PackageValue getPackageMetadata(long packageId) {
    return packageMetadata.get(packageId);
  }

  /**
   * Gets all package metadata.
   *
   * @return Map of package ID to package metadata
   */
  public Map<Long, PackageValue> getAllPackageMetadata() {
    return new HashMap<>(packageMetadata);
  }

  /**
   * Lists all available package files in the Files database.
   *
   * @return Map of package ID to file path
   */
  public Map<Long, String> listPackageFiles() {
    Map<Long, String> packageFiles = new HashMap<>();

    try {
      Path packagesDir = Paths.get(dbPath, "files", "packages");
      if (Files.exists(packagesDir)) {
        Files.list(packagesDir)
            .filter(path -> path.toString().endsWith(".pack"))
            .forEach(
                path -> {
                  try {
                    String fileName = path.getFileName().toString();
                    String packageIdStr = fileName.substring(0, fileName.lastIndexOf('.'));
                    long packageId = Long.parseLong(packageIdStr);
                    packageFiles.put(packageId, path.toString());
                  } catch (Exception e) {
                    log.debug("Error parsing package file name: {}", path.getFileName());
                  }
                });
      }
    } catch (IOException e) {
      log.warn("Error listing package files: {}", e.getMessage());
    }

    return packageFiles;
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
    // Close all package readers
    for (PackageReader reader : packageReaders.values()) {
      try {
        reader.close();
      } catch (IOException e) {
        log.warn("Error closing package reader: {}", e.getMessage());
      }
    }
    packageReaders.clear();

    // Close global index database
    if (globalIndexDb != null) {
      globalIndexDb.close();
    }

    log.info("FilesDbReader closed");
  }
}
