package org.ton.ton4j.tl.types.db;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.tl.types.db.files.GlobalIndexValue;
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
public class GlobalIndexDbReader implements Closeable {

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
  public GlobalIndexDbReader(String dbPath) throws IOException {
    this.dbPath = dbPath;
    initializeFilesDatabase();
    loadMainIndex();
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

  /**
   * Loads the main index entry using db.files.index.key (empty key). This contains the list of all
   * packages in the Files database.
   */
  private void loadMainIndex() throws IOException {
    try {

      AtomicBoolean found = new AtomicBoolean(false);
      // Search for the main index entry by iterating through all entries
      globalIndexDb.forEach(
          (key, value) -> {
            if (found.get()) return; // Already found, skip

            try {
              // Try to deserialize as IndexKey to see if it's the main index entry
              org.ton.ton4j.tl.types.db.files.GlobalIndexKey globalKey =
                  org.ton.ton4j.tl.types.db.files.GlobalIndexKey.deserialize(key);

              if (globalKey instanceof org.ton.ton4j.tl.types.db.files.key.IndexKey) {
                mainIndexIndexValue = (IndexValue) GlobalIndexValue.deserialize(value);

                log.info(
                    "Loaded Files database main index: {} packages, {} key packages, {} temp packages",
                    mainIndexIndexValue.getPackages().size(),
                    mainIndexIndexValue.getKeyPackages().size(),
                    mainIndexIndexValue.getTempPackages().size());
                found.set(true);
              }
            } catch (Exception e) {
              // Not a TL-serialized key, skip
            }
          });

      if (mainIndexIndexValue == null) {
        log.warn("Main index entry not found in Files database");
        // Create empty main index
        mainIndexIndexValue =
            IndexValue.builder()
                .packages(List.of())
                .keyPackages(List.of())
                .tempPackages(List.of())
                .build();
      }
    } catch (Exception e) {
      throw new IOException("Error loading main index: " + e.getMessage(), e);
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
   * Gets archive package file paths from the global index. This method discovers archive package
   * files by scanning the archive/packages directory structure and matching them with package IDs
   * from the global index.
   *
   * @return List of archive package file paths
   */
  public List<String> getArchivePackageFilePaths() {
    List<String> archivePackageFiles = new ArrayList<>();

    if (mainIndexIndexValue == null) {
      log.warn("Main index not loaded, cannot get archive package file paths");
      return archivePackageFiles;
    }

    // Get all package IDs from the global index
    List<Integer> allPackageIds = new ArrayList<>();
    allPackageIds.addAll(mainIndexIndexValue.getPackages());
    allPackageIds.addAll(mainIndexIndexValue.getKeyPackages());
    allPackageIds.addAll(mainIndexIndexValue.getTempPackages());

    log.info("Looking for {} package IDs in archive directories", allPackageIds.size());

    // Get archive packages directory path
    Path archivePackagesDir = Paths.get(dbPath, "archive", "packages");

    if (!Files.exists(archivePackagesDir)) {
      log.warn("Archive packages directory not found: {}", archivePackagesDir);
      return archivePackageFiles;
    }

    // Scan for archive directories (arch0000, arch0001, etc.)
    try {
      Files.list(archivePackagesDir)
          .filter(Files::isDirectory)
          .filter(path -> path.getFileName().toString().startsWith("arch"))
          .forEach(
              archDir -> {
                log.debug("Scanning archive directory: {}", archDir);

                // For each package ID, look for corresponding files in this archive directory
                for (Integer packageId : allPackageIds) {
                  List<String> packageFiles = findPackageFiles(archDir, packageId);
                  archivePackageFiles.addAll(packageFiles);
                }
              });
    } catch (IOException e) {
      log.error("Error scanning archive packages directory: {}", e.getMessage());
    }

    log.info("Found {} archive package files from global index", archivePackageFiles.size());
    return archivePackageFiles;
  }

  /**
   * Gets ALL archive package file paths by scanning the archive directories directly. This method
   * finds all .pack files in archive directories, including those not listed in the Files database
   * global index. This solves the problem of missing archive files like archive.00100.pack that
   * exist in the filesystem but aren't referenced in the Files database.
   *
   * @return List of all archive package file paths found in the filesystem
   */
  public List<String> getAllArchivePackageFilePathsFromFilesystem() {
    List<String> archivePackageFiles = new ArrayList<>();

    // Get archive packages directory path
    Path archivePackagesDir = Paths.get(dbPath, "archive", "packages");

    if (!Files.exists(archivePackagesDir)) {
      log.warn("Archive packages directory not found: {}", archivePackagesDir);
      return archivePackageFiles;
    }

    // Scan for archive directories (arch0000, arch0001, etc.)
    try {
      Files.list(archivePackagesDir)
          .filter(Files::isDirectory)
          .filter(path -> path.getFileName().toString().startsWith("arch"))
          .forEach(
              archDir -> {
                log.debug("Scanning archive directory for all files: {}", archDir);

                try {
                  // Find all .pack files in this archive directory
                  Files.list(archDir)
                      .filter(Files::isRegularFile)
                      .filter(path -> path.getFileName().toString().endsWith(".pack"))
                      .forEach(
                          packFile -> {
                            archivePackageFiles.add(packFile.toString());
                            //                        log.debug("Found archive package file: {}",
                            // packFile);
                          });
                } catch (IOException e) {
                  log.debug("Error scanning archive directory {}: {}", archDir, e.getMessage());
                }
              });
    } catch (IOException e) {
      log.error("Error scanning archive packages directory: {}", e.getMessage());
    }

    log.info("Found {} total archive package files in filesystem", archivePackageFiles.size());
    return archivePackageFiles;
  }

  /**
   * Gets ALL archive file locations by reading individual archive index databases. This method
   * follows the C++ implementation approach by opening each archive.XXXXX.index database and
   * reading the hash->offset mappings. This provides complete access to all files stored in archive
   * packages, including those not referenced in the Files database.
   *
   * @return Map of file hash to ArchiveFileLocation
   */
  public Map<String, ArchiveFileLocation> getAllArchiveFileLocationsFromIndexDatabases() {
    Map<String, ArchiveFileLocation> fileLocations = new HashMap<>();

    // Get archive packages directory path
    Path archivePackagesDir = Paths.get(dbPath, "archive", "packages");

    if (!Files.exists(archivePackagesDir)) {
      log.warn("Archive packages directory not found: {}", archivePackagesDir);
      return fileLocations;
    }

    int totalPackages = 0;
    int totalFiles = 0;

    // Scan for archive directories (arch0000, arch0001, etc.)
    try {
      Files.list(archivePackagesDir)
          .filter(Files::isDirectory)
          .filter(path -> path.getFileName().toString().startsWith("arch"))
          .forEach(
              archDir -> {
                log.debug("Scanning archive directory for index databases: {}", archDir);

                try {
                  // Find all .pack files and their corresponding .index databases
                  Files.list(archDir)
                      .filter(Files::isRegularFile)
                      .filter(path -> path.getFileName().toString().endsWith(".pack"))
                      .forEach(
                          packFile -> {
                            String packFileName = packFile.getFileName().toString();
                            String indexFileName = packFileName.replace(".pack", ".index");
                            Path indexPath = archDir.resolve(indexFileName);

                            if (Files.exists(indexPath)) {
                              try {
                                int packageId = extractPackageIdFromFilename(packFileName);
                                Map<String, ArchiveFileLocation> packageFiles =
                                    readArchiveIndexDatabase(
                                        packFile.toString(), indexPath.toString(), packageId);
                                fileLocations.putAll(packageFiles);

                                log.debug(
                                    "Loaded {} files from archive index: {}",
                                    packageFiles.size(),
                                    indexPath);
                              } catch (Exception e) {
                                log.debug(
                                    "Error reading archive index {}: {}",
                                    indexPath,
                                    e.getMessage());
                              }
                            } else {
                              log.debug("No index database found for package: {}", packFile);
                            }
                          });
                } catch (IOException e) {
                  log.debug("Error scanning archive directory {}: {}", archDir, e.getMessage());
                }
              });
    } catch (IOException e) {
      log.error("Error scanning archive packages directory: {}", e.getMessage());
    }

    log.info(
        "Found {} total files from {} archive index databases",
        fileLocations.size(),
        totalPackages);
    return fileLocations;
  }

  /**
   * Reads hash->offset mappings from a specific archive index database.
   *
   * @param packagePath Path to the package file
   * @param indexPath Path to the index database
   * @param packageId Package ID
   * @return Map of file hash to ArchiveFileLocation
   */
  private Map<String, ArchiveFileLocation> readArchiveIndexDatabase(
      String packagePath, String indexPath, int packageId) {
    Map<String, ArchiveFileLocation> fileLocations = new HashMap<>();

    try (ArchiveIndexReader indexReader =
        new ArchiveIndexReader(indexPath, packagePath, packageId)) {
      Map<String, Long> hashOffsetMap = indexReader.getAllHashOffsetMappings();

      for (Map.Entry<String, Long> entry : hashOffsetMap.entrySet()) {
        String hash = entry.getKey();
        Long offset = entry.getValue();

        ArchiveFileLocation location =
            ArchiveFileLocation.create(packagePath, indexPath, hash, packageId, offset);

        fileLocations.put(hash, location);
      }

    } catch (IOException e) {
      log.debug("Error reading archive index database {}: {}", indexPath, e.getMessage());
    }

    return fileLocations;
  }

  /**
   * Extracts package ID from archive filename. Examples: archive.00100.pack -> 100,
   * archive.00281.0:8000000000000000.pack -> 281
   *
   * @param filename Archive filename
   * @return Package ID
   */
  private int extractPackageIdFromFilename(String filename) {
    try {
      // Handle both formats: archive.00100.pack and archive.00281.0:8000000000000000.pack
      String[] parts = filename.split("\\.");
      if (parts.length >= 2) {
        String packageIdStr = parts[1]; // "00100" or "00281"
        return Integer.parseInt(packageIdStr);
      }
    } catch (Exception e) {
      log.debug("Error extracting package ID from filename {}: {}", filename, e.getMessage());
    }
    return -1;
  }

  /**
   * Gets a summary of archive packages and their file counts by reading index databases.
   *
   * @return Map of package ID to file count
   */
  public Map<Integer, Integer> getArchivePackageFileCounts() {
    Map<Integer, Integer> packageFileCounts = new HashMap<>();

    Path archivePackagesDir = Paths.get(dbPath, "archive", "packages");
    if (!Files.exists(archivePackagesDir)) {
      return packageFileCounts;
    }

    try {
      Files.list(archivePackagesDir)
          .filter(Files::isDirectory)
          .filter(path -> path.getFileName().toString().startsWith("arch"))
          .forEach(
              archDir -> {
                try {
                  Files.list(archDir)
                      .filter(Files::isRegularFile)
                      .filter(path -> path.getFileName().toString().endsWith(".pack"))
                      .forEach(
                          packFile -> {
                            String packFileName = packFile.getFileName().toString();
                            String indexFileName = packFileName.replace(".pack", ".index");
                            Path indexPath = archDir.resolve(indexFileName);

                            if (Files.exists(indexPath)) {
                              try {
                                int packageId = extractPackageIdFromFilename(packFileName);
                                try (ArchiveIndexReader indexReader =
                                    new ArchiveIndexReader(
                                        indexPath.toString(), packFile.toString(), packageId)) {
                                  int fileCount = indexReader.getFileCount();
                                  packageFileCounts.put(packageId, fileCount);
                                }
                              } catch (Exception e) {
                                log.debug(
                                    "Error reading archive index {}: {}",
                                    indexPath,
                                    e.getMessage());
                              }
                            }
                          });
                } catch (IOException e) {
                  log.debug("Error scanning archive directory {}: {}", archDir, e.getMessage());
                }
              });
    } catch (IOException e) {
      log.error("Error scanning archive packages directory: {}", e.getMessage());
    }

    return packageFileCounts;
  }

  /**
   * Reads a file from archive packages using hash->offset lookup. This follows the C++
   * implementation approach where files are accessed by their hash using the individual archive
   * index databases.
   *
   * @param hash The file hash (hex string)
   * @return The file data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public byte[] readArchiveFileByHash(String hash) throws IOException {
    log.debug("Reading archive file by hash: {}", hash);

    // Get all archive file locations
    Map<String, ArchiveFileLocation> fileLocations = getAllArchiveFileLocationsFromIndexDatabases();

    // Find the file location for this hash
    ArchiveFileLocation location = fileLocations.get(hash);
    if (location == null) {
      log.debug("File with hash {} not found in archive indexes", hash);
      return null;
    }

    if (!location.isValid()) {
      log.warn("Invalid location data for hash {}", hash);
      return null;
    }

    // Read from the archive package file
    return readArchiveFileFromPackage(location);
  }

  /**
   * Reads a file from an archive package using ArchiveFileLocation. This method opens the package
   * file and reads data at the specified offset.
   *
   * @param location The ArchiveFileLocation containing package path and offset
   * @return The file data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public byte[] readArchiveFileFromPackage(ArchiveFileLocation location) throws IOException {
    if (location == null || !location.isValid()) {
      return null;
    }

    log.debug(
        "Reading from archive package: {}, offset: {}",
        location.getPackageId(),
        location.getOffset());

    // Get package reader for the archive package
    PackageReader packageReader = getPackageReader(location.getPackagePath());

    // Read entry at the specified offset
    PackageReader.PackageEntry entry = packageReader.getEntryAt(location.getOffset());
    if (entry == null) {
      log.warn(
          "No entry found at offset {} in archive package {}",
          location.getOffset(),
          location.getPackageId());
      return null;
    }

    log.debug(
        "Successfully read {} bytes from archive package {}",
        entry.getData().length,
        location.getPackageId());

    return entry.getData();
  }

  /**
   * Gets all available file hashes from archive index databases. This provides a complete list of
   * all files that can be accessed using the hash->offset lookup mechanism.
   *
   * @return Set of file hashes available in archive packages
   */
  public java.util.Set<String> getAllAvailableArchiveFileHashes() {
    Map<String, ArchiveFileLocation> fileLocations = getAllArchiveFileLocationsFromIndexDatabases();
    return fileLocations.keySet();
  }

  /**
   * Checks if a file with the given hash exists in archive packages.
   *
   * @param hash The file hash to check
   * @return true if the file exists, false otherwise
   */
  public boolean archiveFileExists(String hash) {
    Map<String, ArchiveFileLocation> fileLocations = getAllArchiveFileLocationsFromIndexDatabases();
    return fileLocations.containsKey(hash);
  }

  /**
   * Gets all blocks from archive packages by reading from individual archive index databases. This
   * method follows the same approach as ArchiveDbReader.getAllBlocks() but uses the C++ approach of
   * reading from individual archive index databases.
   *
   * @return List of all blocks found in archive packages
   */
  public List<org.ton.ton4j.tlb.Block> getAllIndexedBlocks() {
    List<org.ton.ton4j.tlb.Block> blocks = new ArrayList<>();

    log.info("Reading all blocks from archive index databases...");

    // Get all archive file locations from index databases
    Map<String, ArchiveFileLocation> fileLocations = getAllArchiveFileLocationsFromIndexDatabases();

    int totalFiles = fileLocations.size();
    int processedFiles = 0;
    int blockCount = 0;
    int errorCount = 0;

    log.info("Found {} files in archive index databases, checking for blocks...", totalFiles);

    for (Map.Entry<String, ArchiveFileLocation> entry : fileLocations.entrySet()) {
      String hash = entry.getKey();
      ArchiveFileLocation location = entry.getValue();

      try {
        processedFiles++;

        // Read the file data using the archive file location
        byte[] fileData = readArchiveFileFromPackage(location);
        if (fileData != null) {
          // Try to parse as BOC and check if it's a block
          org.ton.ton4j.cell.Cell cell =
              org.ton.ton4j.cell.CellBuilder.beginCell().fromBoc(fileData).endCell();

          long magic = cell.getBits().preReadUint(32).longValue();
          if (magic == 0x11ef55aaL) { // block magic
            org.ton.ton4j.tlb.Block block =
                org.ton.ton4j.tlb.Block.deserialize(org.ton.ton4j.cell.CellSlice.beginParse(cell));
            blocks.add(block);
            blockCount++;

            // Log progress for large datasets
            if (blockCount % 100 == 0) {
              log.debug("Processed {} files, found {} blocks so far", processedFiles, blockCount);
            }
          }
        }

      } catch (Throwable e) {
        errorCount++;
        log.debug("Error processing file {}: {}", hash, e.getMessage());
      }
    }

    log.info(
        "Processed {} files from archive index databases: {} blocks found, {} errors",
        processedFiles,
        blockCount,
        errorCount);

    return blocks;
  }

  /**
   * Gets all blocks with their hashes from archive packages. This method is similar to
   * getAllBlocks() but returns a map of hash to block.
   *
   * @return Map of file hash to Block
   */
  public Map<String, org.ton.ton4j.tlb.Block> getAllBlocksWithHashes() {
    Map<String, org.ton.ton4j.tlb.Block> blocks = new HashMap<>();

    log.info("Reading all blocks with hashes from archive index databases...");

    // Get all archive file locations from index databases
    Map<String, ArchiveFileLocation> fileLocations = getAllArchiveFileLocationsFromIndexDatabases();

    int totalFiles = fileLocations.size();
    int processedFiles = 0;
    int blockCount = 0;
    int errorCount = 0;

    log.info("Found {} files in archive index databases, checking for blocks...", totalFiles);

    for (Map.Entry<String, ArchiveFileLocation> entry : fileLocations.entrySet()) {
      String hash = entry.getKey();
      ArchiveFileLocation location = entry.getValue();

      try {
        processedFiles++;

        // Read the file data using the archive file location
        byte[] fileData = readArchiveFileFromPackage(location);
        if (fileData != null) {
          // Try to parse as BOC and check if it's a block
          org.ton.ton4j.cell.Cell cell =
              org.ton.ton4j.cell.CellBuilder.beginCell().fromBoc(fileData).endCell();

          long magic = cell.getBits().preReadUint(32).longValue();
          if (magic == 0x11ef55aaL) { // block magic
            org.ton.ton4j.tlb.Block block =
                org.ton.ton4j.tlb.Block.deserialize(org.ton.ton4j.cell.CellSlice.beginParse(cell));
            blocks.put(hash, block);
            blockCount++;

            // Log progress for large datasets
            if (blockCount % 100 == 0) {
              log.debug("Processed {} files, found {} blocks so far", processedFiles, blockCount);
            }
          }
        }

      } catch (Throwable e) {
        errorCount++;
        log.debug("Error processing file {}: {}", hash, e.getMessage());
      }
    }

    log.info(
        "Processed {} files from archive index databases: {} blocks found, {} errors",
        processedFiles,
        blockCount,
        errorCount);

    return blocks;
  }

  /**
   * Gets all raw entries (file data) from archive packages. This method returns all files found in
   * archive index databases as raw byte arrays.
   *
   * @return Map of file hash to raw file data
   */
  public Map<String, byte[]> getAllArchiveEntries() {
    Map<String, byte[]> entries = new HashMap<>();

    log.info("Reading all entries from archive index databases...");

    // Get all archive file locations from index databases
    Map<String, ArchiveFileLocation> fileLocations = getAllArchiveFileLocationsFromIndexDatabases();

    int totalFiles = fileLocations.size();
    int processedFiles = 0;
    int successCount = 0;
    int errorCount = 0;

    log.info("Found {} files in archive index databases, reading all entries...", totalFiles);

    for (Map.Entry<String, ArchiveFileLocation> entry : fileLocations.entrySet()) {
      String hash = entry.getKey();
      ArchiveFileLocation location = entry.getValue();

      try {
        processedFiles++;

        // Read the file data using the archive file location
        byte[] fileData = readArchiveFileFromPackage(location);
        if (fileData != null) {
          entries.put(hash, fileData);
          successCount++;

          // Log progress for large datasets
          if (successCount % 1000 == 0) {
            log.debug("Processed {} files, read {} entries so far", processedFiles, successCount);
          }
        }

      } catch (Exception e) {
        errorCount++;
        log.debug("Error reading file {}: {}", hash, e.getMessage());
      }
    }

    log.info(
        "Processed {} files from archive index databases: {} entries read, {} errors",
        processedFiles,
        successCount,
        errorCount);

    return entries;
  }

  /**
   * Finds all package files for a given package ID in a specific archive directory. This includes
   * both main package files (archive.{packageId:05d}.pack) and shard-specific files
   * (archive.{packageId:05d}.{workchain}:{shard}.pack).
   *
   * @param archDir The archive directory to search in
   * @param packageId The package ID to look for
   * @return List of file paths for this package ID
   */
  private List<String> findPackageFiles(Path archDir, Integer packageId) {
    List<String> packageFiles = new ArrayList<>();

    try {
      String packageIdFormatted = String.format("%05d", packageId);

      // Look for files matching the package ID pattern
      Files.list(archDir)
          .filter(Files::isRegularFile)
          .filter(
              path -> {
                String fileName = path.getFileName().toString();
                return fileName.startsWith("archive." + packageIdFormatted)
                    && fileName.endsWith(".pack");
              })
          .forEach(
              path -> {
                packageFiles.add(path.toString());
                //                log.debug("Found package file for ID {}: {}", packageId, path);
              });

    } catch (IOException e) {
      log.debug(
          "Error scanning archive directory {} for package {}: {}",
          archDir,
          packageId,
          e.getMessage());
    }

    return packageFiles;
  }

  /**
   * Gets the archive package file path for a specific package ID. This method searches through all
   * archive directories to find the package file.
   *
   * @param packageId The package ID
   * @return The file path or null if not found
   */
  public String getArchivePackageFilePath(long packageId) {
    Path archivePackagesDir = Paths.get(dbPath, "archive", "packages");

    if (!Files.exists(archivePackagesDir)) {
      log.debug("Archive packages directory not found: {}", archivePackagesDir);
      return null;
    }

    try {
      // Search through all archive directories
      return Files.list(archivePackagesDir)
          .filter(Files::isDirectory)
          .filter(path -> path.getFileName().toString().startsWith("arch"))
          .map(archDir -> findMainPackageFile(archDir, packageId))
          .filter(path -> path != null)
          .findFirst()
          .orElse(null);
    } catch (IOException e) {
      log.debug("Error searching for package {} file: {}", packageId, e.getMessage());
      return null;
    }
  }

  /**
   * Finds the main package file (archive.{packageId:05d}.pack) for a given package ID in a specific
   * archive directory.
   *
   * @param archDir The archive directory to search in
   * @param packageId The package ID to look for
   * @return The file path or null if not found
   */
  private String findMainPackageFile(Path archDir, long packageId) {
    String packageFileName = String.format("archive.%05d.pack", packageId);
    Path packagePath = archDir.resolve(packageFileName);

    if (Files.exists(packagePath)) {
      log.debug("Found main package file for ID {}: {}", packageId, packagePath);
      return packagePath.toString();
    }

    return null;
  }

  /**
   * Gets all archive package file paths for a specific package ID, including shard-specific files.
   * This method searches through all archive directories to find all files related to the package.
   *
   * @param packageId The package ID
   * @return List of file paths for this package ID
   */
  public List<String> getAllArchivePackageFilePaths(long packageId) {
    List<String> packageFiles = new ArrayList<>();
    Path archivePackagesDir = Paths.get(dbPath, "archive", "packages");

    if (!Files.exists(archivePackagesDir)) {
      log.debug("Archive packages directory not found: {}", archivePackagesDir);
      return packageFiles;
    }

    try {
      // Search through all archive directories
      Files.list(archivePackagesDir)
          .filter(Files::isDirectory)
          .filter(path -> path.getFileName().toString().startsWith("arch"))
          .forEach(
              archDir -> {
                List<String> files = findPackageFiles(archDir, (int) packageId);
                packageFiles.addAll(files);
              });
    } catch (IOException e) {
      log.debug("Error searching for package {} files: {}", packageId, e.getMessage());
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
