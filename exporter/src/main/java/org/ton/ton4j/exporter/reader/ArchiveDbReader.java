package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.exporter.types.ArchiveInfo;

/** Specialized reader for TON archive database. */
@Slf4j
@Data
public class ArchiveDbReader implements Closeable {

  private final String rootPath;
  String dbPath;
  private final Map<String, RocksDbWrapper> indexDbs = new HashMap<>();
  private final Map<String, PackageReader> packageReaders = new HashMap<>();
  private final Map<String, ArchiveInfo> archiveInfos = new HashMap<>();
  private GlobalIndexDbReader globalIndexDbReader;
  private final Map<String, PackageReader> filesPackageReaders = new HashMap<>();

  /**
   * Creates a new ArchiveDbReader.
   *
   * @param rootPath Path to the archive database directory
   */
  public ArchiveDbReader(String rootPath) {

    this.rootPath = Paths.get(rootPath, "archive").toString();
    this.dbPath = Paths.get(rootPath).toString();

    // Initialize Files database global index
    initializeFilesDatabase(rootPath);

    // Discover archive packages using GlobalIndexDbReader
    discoverArchivesFromGlobalIndex();
  }

  private void discoverArchivesFromGlobalIndex() {
    // Use comprehensive filesystem scanning instead of relying on Files database global index
    // This ensures we find ALL archive packages, including those not referenced in the Files
    // database
    discoverAllArchivePackagesFromFilesystem();

    // Also try to discover from global index as fallback for any additional packages
    // but avoid duplicate scanning by using the Files database index directly
    if (globalIndexDbReader != null) {
      discoverArchivesFromFilesDatabase();
    }
  }

  /**
   * Discovers ALL archive packages by directly scanning the filesystem. This method uses the same
   * comprehensive approach as GlobalIndexDbReader to find all .pack files and their corresponding
   * .index databases.
   */
  private void discoverAllArchivePackagesFromFilesystem() {
    log.info("Discovering all archive packages from filesystem (comprehensive scan)...");

    // Get archive packages directory path
    Path archivePackagesDir = Paths.get(rootPath, "packages");

    if (!Files.exists(archivePackagesDir)) {
      log.warn("Archive packages directory not found: {}", archivePackagesDir);
      return;
    }

    try {
      // Scan for archive directories (arch0000, arch0001, etc.)
      Files.list(archivePackagesDir)
          .filter(Files::isDirectory)
          .filter(path -> path.getFileName().toString().startsWith("arch"))
          .forEach(
              archDir -> {
                log.debug("Scanning archive directory: {}", archDir);

                try {
                  // Find all .pack files in this archive directory
                  Files.list(archDir)
                      .filter(Files::isRegularFile)
                      .filter(path -> path.getFileName().toString().endsWith(".pack"))
                      .forEach(
                          packFile -> {
                            try {
                              String packFileName = packFile.getFileName().toString();
                              String indexFileName = packFileName.replace(".pack", ".index");
                              Path indexPath = archDir.resolve(indexFileName);

                              // Extract package info from the file path
                              Path parentDir = packFile.getParent();
                              String dirName = parentDir.getFileName().toString();

                              // Remove .pack extension to get the package base name
                              String packageBaseName =
                                  packFileName.substring(0, packFileName.lastIndexOf('.'));
                              String archiveKey = dirName + "/" + packageBaseName;

                              // Extract archive ID from directory name (arch0000 -> 0)
                              int archiveId = 0;
                              if (dirName.startsWith("arch")) {
                                try {
                                  archiveId = Integer.parseInt(dirName.substring(4));
                                } catch (NumberFormatException e) {
                                  log.debug(
                                      "Could not parse archive ID from directory name: {}",
                                      dirName);
                                }
                              }

                              // Check if index file exists
                              String indexPathStr =
                                  Files.exists(indexPath) ? indexPath.toString() : null;

                              // Create archive info
                              archiveInfos.put(
                                  archiveKey,
                                  new ArchiveInfo(archiveId, indexPathStr, packFile.toString()));

                              //                              log.debug(
                              //                                  "Discovered archive package: {}",
                              //                                  archiveKey);

                            } catch (Exception e) {
                              log.debug(
                                  "Error processing archive package file {}: {}",
                                  packFile,
                                  e.getMessage());
                            }
                          });
                } catch (IOException e) {
                  log.debug("Error scanning archive directory {}: {}", archDir, e.getMessage());
                }
              });
    } catch (IOException e) {
      log.error("Error scanning archive packages directory: {}", e.getMessage());
    }

    log.info("Discovered {} total archive packages from filesystem", archiveInfos.size());
  }

  /**
   * Discovers archives from the Files database global index (fallback method). This is kept as a
   * fallback in case there are additional packages referenced in the Files database that weren't
   * found by filesystem scanning. This method uses the Files database index directly without
   * additional filesystem scanning to avoid duplication.
   */
  private void discoverArchivesFromFilesDatabase() {
    log.debug("Discovering additional archives from Files database global index...");

    // Use the Files database index directly to get package information
    // without additional filesystem scanning
    try {
      if (globalIndexDbReader.getMainIndexIndexValue() != null) {
        List<Integer> allPackageIds = new ArrayList<>();
        allPackageIds.addAll(globalIndexDbReader.getMainIndexIndexValue().getPackages());
        allPackageIds.addAll(globalIndexDbReader.getMainIndexIndexValue().getKeyPackages());
        allPackageIds.addAll(globalIndexDbReader.getMainIndexIndexValue().getTempPackages());

        int newPackages = 0;

        // For each package ID, try to find corresponding archive files that weren't found by
        // filesystem scan
        for (Integer packageId : allPackageIds) {
          // Look for archive files with this package ID that might have been missed
          String archiveKey = "files/" + String.format("%010d", packageId);

          if (!archiveInfos.containsKey(archiveKey)) {
            // Try to find the package file path
            String packagePath = globalIndexDbReader.getPackageFilePath(packageId);
            if (packagePath != null) {
              // Create archive info for Files database package (no index file)
              archiveInfos.put(archiveKey, new ArchiveInfo(packageId, null, packagePath));
              newPackages++;

              log.debug(
                  "Discovered additional Files database package: {} (package: {})",
                  archiveKey,
                  packagePath);
            }
          }
        }

        if (newPackages > 0) {
          log.info("Discovered {} additional packages from Files database index", newPackages);
        }
      }
    } catch (Exception e) {
      log.debug("Error discovering archives from Files database index: {}", e.getMessage());
    }
  }

  /** Initializes the Files database global index. */
  private void initializeFilesDatabase(String rootPath) {
    // Check if files database exists - it should be at the same level as archive
    Path globalIndexPath = Paths.get(rootPath);

    if (Files.exists(globalIndexPath)) {
      try {
        globalIndexDbReader = new GlobalIndexDbReader(globalIndexPath.toString());
        log.info("Initialized Files database reader: {}", globalIndexPath);
      } catch (IOException e) {
        log.warn("Could not initialize Files database reader: {}", e.getMessage());
      }
    } else {
      log.info("Files database not found at: {}", globalIndexPath);
    }
  }

  /**
   * Gets all available archive keys.
   *
   * @return List of archive keys
   */
  public List<String> getArchiveKeys() {
    return new ArrayList<>(archiveInfos.keySet());
  }

  //  public Map<String, Block> getAllBlocksWithHashes() {
  //    Map<String, Block> blocks = new HashMap<>();
  //    Map<String, byte[]> entries = getAllEntries();
  //    for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
  //      try {
  //        Cell c = CellBuilder.beginCell().fromBoc(entry.getValue()).endCell();
  //        long magic = c.getBits().preReadUint(32).longValue();
  //        if (magic == 0x11ef55aaL) { // block
  //          Block block = Block.deserialize(CellSlice.beginParse(c));
  //          blocks.put(entry.getKey(), block);
  //        }
  //      } catch (Throwable e) {
  //        log.error("Error parsing block {}", e.getMessage());
  //      }
  //    }
  //    return blocks;
  //  }

  /**
   * Gets all blocks from all archives.
   *
   * @return Map of block hash to block data
   */
  public Map<String, byte[]> getAllEntries() {
    Map<String, byte[]> blocks = new HashMap<>();

    // Iterate through all archives
    for (Map.Entry<String, ArchiveInfo> entry : archiveInfos.entrySet()) {
      String archiveKey = entry.getKey();
      ArchiveInfo archiveInfo = entry.getValue();

      try {
        // Check if this is a Files database package (indicated by null indexPath)
        if (archiveInfo.getIndexPath() == null) {
          // This is a Files database package, handle it separately
          readFromFilesPackage(archiveKey, archiveInfo, blocks);
        } else {
          // This is a traditional archive package with its own index
          readFromTraditionalArchive(archiveKey, archiveInfo, blocks);
        }
      } catch (IOException e) {
        log.warn("Error reading blocks from archive {}: {}", archiveKey, e.getMessage());
      }
    }

    return blocks;
  }

  //  /**
  //   * Gets all blocks from all archives using parallel processing. Uses 32 threads by default for
  //   * concurrent reading from multiple archive packages.
  //   *
  //   * @return Map of block hash to block data
  //   */
  //  public Map<String, byte[]> getAllEntriesParallel() {
  //    return getAllEntriesParallel(32);
  //  }

  //  /**
  //   * Gets all blocks from all archives using parallel processing with configurable thread count.
  //   *
  //   * @param threadCount Number of threads to use for parallel processing
  //   * @return Map of block hash to block data
  //   */
  //  public Map<String, byte[]> getAllEntriesParallel(int threadCount) {
  //    if (threadCount <= 1) {
  //      // Fall back to single-threaded version
  //      return getAllEntries();
  //    }
  //
  //    log.info(
  //        "Starting parallel reading from {} archives using {} threads",
  //        archiveInfos.size(),
  //        threadCount);
  //    long startTime = System.currentTimeMillis();
  //
  //    // Use ConcurrentHashMap for thread-safe operations
  //    Map<String, byte[]> blocks = new ConcurrentHashMap<>();
  //
  //    // Create thread pool
  //    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
  //    List<Future<Void>> futures = new ArrayList<>();
  //
  //    // Submit tasks for each archive
  //    for (Map.Entry<String, ArchiveInfo> entry : archiveInfos.entrySet()) {
  //      String archiveKey = entry.getKey();
  //      ArchiveInfo archiveInfo = entry.getValue();
  //
  //      Future<Void> future =
  //          executor.submit(
  //              () -> {
  //                try {
  //                  // Create a local map for this thread's results
  //                  Map<String, byte[]> localBlocks = new HashMap<>();
  //
  //                  // Check if this is a Files database package (indicated by null indexPath)
  //                  if (archiveInfo.getIndexPath() == null) {
  //                    // This is a Files database package, handle it separately
  //                    readFromFilesPackage(archiveKey, archiveInfo, localBlocks);
  //                  } else {
  //                    // This is a traditional archive package with its own index
  //                    readFromTraditionalArchive(archiveKey, archiveInfo, localBlocks);
  //                  }
  //
  //                  // Merge results into the concurrent map
  //                  blocks.putAll(localBlocks);
  //
  //                  log.debug(
  //                      "Completed reading archive {}: {} entries", archiveKey,
  // localBlocks.size());
  //
  //                } catch (Exception e) {
  //                  log.error("Unexpected error reading archive {}: {}", archiveKey,
  // e.getMessage());
  //                }
  //                return null;
  //              });
  //
  //      futures.add(future);
  //    }
  //
  //    // Wait for all tasks to complete
  //    for (Future<Void> future : futures) {
  //      try {
  //        future.get();
  //      } catch (Exception e) {
  //        log.error("Error waiting for archive reading task: {}", e.getMessage());
  //      }
  //    }
  //
  //    // Shutdown executor
  //    executor.shutdown();
  //    try {
  //      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
  //        executor.shutdownNow();
  //      }
  //    } catch (InterruptedException e) {
  //      executor.shutdownNow();
  //      Thread.currentThread().interrupt();
  //    }
  //
  //    long endTime = System.currentTimeMillis();
  //    long durationMs = endTime - startTime;
  //    double durationSeconds = durationMs / 1000.0;
  //
  //    log.info(
  //        "Parallel reading completed: {} entries in {} ms ({} seconds) using {} threads",
  //        blocks.size(),
  //        durationMs,
  //        String.format("%.2f", durationSeconds),
  //        threadCount);
  //
  //    return blocks;
  //  }

  /** Reads blocks from a traditional archive package with its own index. */
  public void readFromTraditionalArchive(
      String archiveKey, ArchiveInfo archiveInfo, Map<String, byte[]> blocks) throws IOException {
    // Get the index DB
    RocksDbWrapper indexDb = getIndexDb(archiveKey, archiveInfo.getIndexPath());

    // Get all key-value pairs from this index
    indexDb.forEach(
        (key, value) -> {
          try {
            // Skip keys that are not valid hex strings (likely system or metadata keys)
            String hash = new String(key);
            if (!isValidHexString(hash) && !hash.equals("status")) {
              return;
            }

            // Validate the value before parsing
            if (value == null || value.length == 0) {
              log.warn(
                  "Invalid value for key {} in archive {}: value is null or empty",
                  hash,
                  archiveKey);
              return;
            }

            long offset;
            try {
              // Try to parse the offset as a string first (as in C++ implementation)
              String offsetStr = new String(value);
              try {
                offset = Long.parseLong(offsetStr.trim());
              } catch (NumberFormatException e) {
                // If string parsing fails, try binary format as fallback
                if (value.length >= 8) {
                  offset = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).getLong();
                } else {
                  return;
                }
              }
            } catch (Exception e) {
              log.warn(
                  "Error parsing offset for key {} in archive {}: {}",
                  hash,
                  archiveKey,
                  e.getMessage());
              return;
            }

            // Validate the offset
            if (offset < 0) {
              log.warn(
                  "Negative seek offset {} for key {} in archive {}", offset, hash, archiveKey);
              return;
            }

            try {
              // Get the package reader
              PackageReader packageReader =
                  getPackageReader(archiveKey, archiveInfo.getPackagePath());

              // Check if packageReader is null before using it
              if (packageReader != null) {
                // Get the entry at the offset
                PackageReader.PackageEntry packageEntry = packageReader.getEntryAt(offset);

                if (packageEntry != null) {
                  blocks.put(hash, packageEntry.getData());
                } else {
                  log.warn(
                      "Null package entry for key {} at offset {} in archive {}",
                      hash,
                      offset,
                      archiveKey);
                }
              } else {
                log.warn(
                    "PackageReader is null for archive {} with package path {}",
                    archiveKey,
                    archiveInfo.getPackagePath());
              }
            } catch (IOException e) {
              // Silently skip individual entry errors
            }
          } catch (Exception e) {
            log.warn(
                "Unexpected error processing key in archive {}: {}", archiveKey, e.getMessage());
          }
        });
  }

  /** Reads blocks from a Files database package using the global index. */
  public void readFromFilesPackage(
      String archiveKey, ArchiveInfo archiveInfo, Map<String, byte[]> blocks) {
    // Check if this is an orphaned package (no index file)
    // This includes both explicitly orphaned packages and archive packages without index files
    if (archiveKey.startsWith("orphaned/") || archiveInfo.getIndexPath() == null) {
      // Read directly from the package file like TestFilesDbReader does
      //      log.debug("Reading orphaned package: {} (no index file)", archiveKey);
      readFromOrphanedPackage(archiveKey, archiveInfo, blocks);
      return;
    }

    if (globalIndexDbReader == null) {
      return;
    }

    // Extract package filename from the archive key (e.g., "files/0000000100" -> "0000000100.pack")
    String packageBaseName = archiveKey.substring(archiveKey.lastIndexOf('/') + 1);
    String packageFileName = packageBaseName + ".pack";

    globalIndexDbReader
        .getGlobalIndexDb()
        .forEach(
            (key, value) -> {
              try {
                String hash = new String(key);
                if (!isValidHexString(hash)) {
                  return; // Skip non-hex keys
                }

                // Parse the value to get package location info
                if (value.length >= 16) { // At least 8 bytes for package_id + 8 bytes for offset
                  ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
                  long packageId = buffer.getLong();
                  long offset = buffer.getLong();

                  // Check if this entry belongs to the current package
                  String entryPackageFileName = String.format("%010d.pack", packageId);
                  if (!entryPackageFileName.equals(packageFileName)) {
                    return; // This entry belongs to a different package
                  }

                  try {
                    PackageReader packageReader =
                        getFilesPackageReader(packageFileName, archiveInfo.getPackagePath());
                    PackageReader.PackageEntry entry = packageReader.getEntryAt(offset);

                    if (entry != null) {
                      blocks.put(hash, entry.getData());
                    }
                  } catch (IOException e) {
                    // Silently skip errors for individual entries
                  }
                }
              } catch (Exception e) {
                // Silently skip errors for individual entries
              }
            });
  }

  /**
   * Reads blocks directly from an orphaned package file (no index file available). This method
   * reads the package file sequentially and extracts all entries.
   */
  private void readFromOrphanedPackage(
      String archiveKey, ArchiveInfo archiveInfo, Map<String, byte[]> blocks) {
    try {
      AtomicInteger entryCount = new AtomicInteger();
      PackageReader packageReader = getPackageReader(archiveKey, archiveInfo.getPackagePath());

      // Check if packageReader is null before using it
      if (packageReader != null) {
        packageReader.forEach(
            packageEntry -> {
              entryCount.getAndIncrement();
              String hash = extractHashFromFilename(packageEntry.getFilename());
              if (hash != null) {
                blocks.put(hash, packageEntry.getData());
              }
            });

        //      log.info(
        //          "Successfully read {} entries from orphaned package: {}",
        //          entryCount,
        //          archiveInfo.getPackagePath());
      } else {
        log.warn(
            "PackageReader is null for orphaned package {} with path {}",
            archiveKey,
            archiveInfo.getPackagePath());
      }

    } catch (IOException e) {
      log.error(
          "Error reading orphaned package {}: {}", archiveInfo.getPackagePath(), e.getMessage());
    }
  }

  /**
   * Extracts hash from a filename like "block_(-1,8000000000000000,100):hash1:hash2". Returns the
   * first hash (hash1) which is typically used as the key.
   */
  private String extractHashFromFilename(String filename) {
    try {
      // Look for pattern like "block_(...):hash" or "proof_(...):hash"
      if (filename.contains("):")) {
        int colonIndex = filename.indexOf("):");
        if (colonIndex != -1) {
          String hashPart = filename.substring(colonIndex + 2);
          // Take the first hash (before the next colon if present)
          int nextColonIndex = hashPart.indexOf(':');
          if (nextColonIndex != -1) {
            return hashPart.substring(0, nextColonIndex);
          } else {
            return hashPart;
          }
        }
      }
    } catch (Exception e) {
      log.warn("Error extracting hash from filename {}: {}", filename, e.getMessage());
    }
    return null;
  }

  /**
   * Reads a specific block from the Files database using the global index.
   *
   * @param hash The block hash to look for
   * @return The block data, or null if not found
   */
  private byte[] readBlockFromFilesDatabase(String hash) {
    if (globalIndexDbReader == null) {
      return null;
    }

    try {
      // Try to get the offset from the global index
      byte[] offsetBytes = globalIndexDbReader.getGlobalIndexDb().get(hash.getBytes());
      if (offsetBytes != null) {
        // Parse the value to get package location info
        if (offsetBytes.length >= 16) { // At least 8 bytes for package_id + 8 bytes for offset
          ByteBuffer buffer = ByteBuffer.wrap(offsetBytes).order(ByteOrder.LITTLE_ENDIAN);
          long packageId = buffer.getLong();
          long offset = buffer.getLong();

          // Validate the offset
          if (offset < 0) {
            log.warn("Negative seek offset {} for key {} in Files database", offset, hash);
            return null;
          }

          // Construct package file path
          String packageFileName = String.format("%010d.pack", packageId);
          Path filesPackagesPath = Paths.get(rootPath, "..", "files", "packages");
          Path packagePath = filesPackagesPath.resolve(packageFileName);

          if (Files.exists(packagePath)) {
            try {
              PackageReader packageReader =
                  getFilesPackageReader(packageFileName, packagePath.toString());
              PackageReader.PackageEntry entry = packageReader.getEntryAt(offset);

              if (entry != null) {
                return entry.getData();
              }
            } catch (IOException e) {
              log.warn(
                  "Error reading block {} from Files package {}: {}",
                  hash,
                  packageFileName,
                  e.getMessage());
            }
          }
        }
      }
    } catch (Exception e) {
      log.warn("Error reading block {} from Files database: {}", hash, e.getMessage());
    }

    return null;
  }

  /** Gets a package reader for Files database packages. */
  private PackageReader getFilesPackageReader(String packageKey, String packagePath)
      throws IOException {
    if (!filesPackageReaders.containsKey(packageKey)) {
      filesPackageReaders.put(packageKey, new PackageReader(packagePath));
    }
    return filesPackageReaders.get(packageKey);
  }

  /** Checks if a RocksDB key represents a block info entry. */
  private boolean isBlockInfoKey(byte[] key) {
    if (key == null || key.length < 4) {
      return false;
    }

    try {
      // Check for TL-serialized keys with the specific db_blockdb_key_value magic number
      ByteBuffer buffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
      int magic = buffer.getInt();

      // Use the correct magic number for db_blockdb_key_value from ton_api.h
      return magic == 0x7f57d173; // db_blockdb_key_value::ID = 2136461683
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Gets an index DB for a specific archive.
   *
   * @param archiveKey The archive key
   * @param indexPath Path to the index file
   * @return The index DB
   * @throws IOException If an I/O error occurs
   */
  private RocksDbWrapper getIndexDb(String archiveKey, String indexPath) throws IOException {
    if (!indexDbs.containsKey(archiveKey)) {
      indexDbs.put(archiveKey, new RocksDbWrapper(indexPath));
    }

    return indexDbs.get(archiveKey);
  }

  /**
   * Gets a package reader for a specific archive.
   *
   * @param archiveKey The archive key
   * @param packagePath Path to the package file
   * @return The package reader, or null if creation fails
   * @throws IOException If an I/O error occurs
   */
  private PackageReader getPackageReader(String archiveKey, String packagePath) throws IOException {
    if (!packageReaders.containsKey(archiveKey)) {
      try {
        PackageReader reader = new PackageReader(packagePath);
        packageReaders.put(archiveKey, reader);
      } catch (IOException e) {
        log.warn(
            "Failed to create PackageReader for archive {} with path {}: {}",
            archiveKey,
            packagePath,
            e.getMessage());
        // Store null to avoid repeated attempts
        packageReaders.put(archiveKey, null);
        throw e;
      }
    }

    return packageReaders.get(archiveKey);
  }

  @Override
  public void close() throws IOException {

    // Close all package readers
    for (PackageReader reader : packageReaders.values()) {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          log.warn("Error closing package reader: {}", e.getMessage());
        }
      }
    }

    for (PackageReader reader : filesPackageReaders.values()) {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          log.warn("Error closing files package reader: {}", e.getMessage());
        }
      }
    }

    // Close global index database reader
    if (globalIndexDbReader != null) {
      try {
        globalIndexDbReader.close();
      } catch (IOException e) {
        log.warn("Error closing global index database reader: {}", e.getMessage());
      }
    }

    // Close index databases
    for (RocksDbWrapper indexDb : indexDbs.values()) {
      if (indexDb != null) {
        try {
          indexDb.close();
        } catch (IOException e) {
          log.warn("Error closing index database: {}", e.getMessage());
        }
      }
    }
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

    // Check if the string contains only hexadecimal characters
    return s.matches("^[0-9A-Fa-f]+$");
  }
}
