package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.exporter.types.ArchiveFileLocation;
import org.ton.ton4j.exporter.types.ArchiveInfo;
import org.ton.ton4j.tl.types.db.files.GlobalIndexKey;
import org.ton.ton4j.tl.types.db.files.GlobalIndexValue;
import org.ton.ton4j.tl.types.db.files.index.IndexValue;
import org.ton.ton4j.tl.types.db.files.key.IndexKey;
import org.ton.ton4j.tl.types.db.files.key.PackageKey;
import org.ton.ton4j.tl.types.db.files.pkg.FirstBlock;
import org.ton.ton4j.tl.types.db.files.pkg.PackageValue;

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

  // Optimized index for fast archive lookup by seqno
  // Key: workchain, Value: TreeMap of (seqno -> packageId)
  private final Map<Integer, TreeMap<Integer, Integer>> packageIndexByWorkchain = new HashMap<>();

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
    buildPackageIndex();
  }

  /**
   * Creates a new FilesDbReader.
   *
   * @param dbPath Path to the database root directory (should contain files/globalindex)
   * @param withPreloadedList if true - builds an optimized in-memory index for fast package lookup
   *     by seqno. This method scans all packages once and creates a TreeMap for each workchain that
   *     maps seqno ranges to package IDs, enabling O(log n) lookups instead of O(n).
   * @throws IOException If an I/O error occurs
   */
  public GlobalIndexDbReader(String dbPath, boolean withPreloadedList) throws IOException {
    this.dbPath = dbPath;
    initializeFilesDatabase();
    loadMainIndex();
    if (withPreloadedList) {
      buildPackageIndex();
    }
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
              GlobalIndexKey globalKey = GlobalIndexKey.deserialize(key);

              if (globalKey instanceof IndexKey) {
                mainIndexIndexValue = (IndexValue) GlobalIndexValue.deserialize(value);

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

  /**
   * Builds an optimized in-memory index for fast package lookup by seqno. This method scans all
   * packages once and creates a TreeMap for each workchain that maps seqno ranges to package IDs,
   * enabling O(log n) lookups instead of O(n).
   */
  private void buildPackageIndex() {
    int packageCount = 0;

    globalIndexDb.forEach(
        (key, value) -> {
          try {
            GlobalIndexKey globalIndexKey = GlobalIndexKey.deserialize(key);
            if (!(globalIndexKey instanceof PackageKey)) {
              return;
            }

            GlobalIndexValue globalIndexValue = GlobalIndexValue.deserialize(value);
            if (!(globalIndexValue instanceof PackageValue)) {
              return;
            }

            PackageValue packageValue = (PackageValue) globalIndexValue;
            if (packageValue.isTemp()) {
              return; // Skip temp packages
            }

            int packageId = ((PackageKey) globalIndexKey).getPackageId();
            List<FirstBlock> firstBlocks = packageValue.getFirstblocks();

            if (firstBlocks == null || firstBlocks.isEmpty()) {
              return;
            }

            // Index each first block by workchain and seqno
            for (FirstBlock firstBlock : firstBlocks) {
              int workchain = firstBlock.getWorkchain();
              int seqno = firstBlock.getSeqno();

              // Get or create TreeMap for this workchain
              TreeMap<Integer, Integer> seqnoMap =
                  packageIndexByWorkchain.computeIfAbsent(workchain, k -> new TreeMap<>());

              // Store the mapping: seqno -> packageId
              // If multiple packages have the same seqno, keep the one with higher packageId
              seqnoMap.merge(seqno, packageId, Math::max);
            }
          } catch (Exception e) {
            log.debug("Error processing package for index: {}", e.getMessage());
          }
        });

    // Count total entries
    for (TreeMap<Integer, Integer> map : packageIndexByWorkchain.values()) {
      packageCount += map.size();
    }
  }

  /** Reads blocks from a Files database package using the global index. */
  public void readFromFilesPackage(
      String archiveKey, ArchiveInfo archiveInfo, Map<String, byte[]> blocks) {

    // Extract package filename from the archive key (e.g., "files/0000000100" -> "0000000100.pack")
    String packageBaseName = archiveKey.substring(archiveKey.lastIndexOf('/') + 1);
    String packageFileName = packageBaseName + ".pack";

    globalIndexDb.forEach(
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
                PackageReader packageReader = getPackageReader(archiveInfo.getPackagePath());
                PackageReader.PackageEntry entryObj = packageReader.getEntryAt(offset);

                if (entryObj != null) {
                  if (entryObj.getFilename().startsWith("block_")) {
                    blocks.put(hash, entryObj.getData());
                  }
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
   * Gets archive package file paths from the global index. This method discovers archive package
   * files by scanning the archive/packages directory structure and matching them with package IDs
   * from the global index.
   *
   * @return List of archive package file paths
   */
  public List<String> getArchivePackagesFromMainIndex() {
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
          //          .filter(path -> path.getFileName().toString().startsWith("arch"))
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
   * global index.
   *
   * @return List of all archive package file paths found in the filesystem
   */
  public List<String> getAllArchivePackageByDirScan() {
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
   * @return might be thousands
   */
  public List<String> getAllPackFiles() {
    List<String> packFilenames = new ArrayList<>();

    // Get archive packages directory path
    Path archivePackagesDir = Paths.get(dbPath, "archive", "packages");

    if (!Files.exists(archivePackagesDir)) {
      log.warn("Archive packages directory not found: {}", archivePackagesDir);
      return packFilenames;
    }

    globalIndexDb.forEach(
        (key, value) -> {
          GlobalIndexKey globalIndexKey = GlobalIndexKey.deserialize(key);
          if (globalIndexKey instanceof PackageKey) {
            GlobalIndexValue globalIndexValue = GlobalIndexValue.deserialize(value);
            PackageValue packageValue = ((PackageValue) globalIndexValue);
            if (!packageValue.isTemp()) { // exclude temp

              int packageId = ((PackageKey) globalIndexKey).getPackageId();

              String absoluteIndexFolder =
                  ArchiveIndexReader.getArchiveIndexPath(dbPath, packageId);

              try (ArchiveIndexReader archiveIndexReader =
                  new ArchiveIndexReader(absoluteIndexFolder)) {
                packFilenames.addAll(archiveIndexReader.getAllPackFiles());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          }
        });

    log.info("Found total pack files {}", packFilenames.size());
    return packFilenames;
  }

  public Map<String, ArchiveFileLocation> getAllPackagesHashOffsetMappings() {
    Map<String, ArchiveFileLocation> packagesHashOffset = new HashMap<>();

    // Get archive packages directory path
    Path archivePackagesDir = Paths.get(dbPath, "archive", "packages");

    if (!Files.exists(archivePackagesDir)) {
      log.warn("Archive packages directory not found: {}", archivePackagesDir);
      return packagesHashOffset;
    }

    globalIndexDb.forEach(
        (key, value) -> {
          GlobalIndexKey globalIndexKey = GlobalIndexKey.deserialize(key);
          if (globalIndexKey instanceof PackageKey) {
            GlobalIndexValue globalIndexValue = GlobalIndexValue.deserialize(value);
            PackageValue packageValue = ((PackageValue) globalIndexValue);
            if (!packageValue.isTemp()) { // exclude temp

              int packageId = ((PackageKey) globalIndexKey).getPackageId();

              String absoluteIndexFolder =
                  ArchiveIndexReader.getArchiveIndexPath(dbPath, packageId);

              Map<String, ArchiveFileLocation> packageFiles =
                  readArchiveIndexDatabase("", absoluteIndexFolder, packageId);
              packagesHashOffset.putAll(packageFiles);
            }
          }
        });

    log.info(
        "Found {} total packages from {} archive index databases",
        packagesHashOffset.size(),
        mainIndexIndexValue.getPackages().size());
    return packagesHashOffset;
  }

  public List<Integer> getAllArchiveIndexesIds() {
    List<Integer> packageIds = new ArrayList<>();

    // Get archive packages directory path
    Path archivePackagesDir = Paths.get(dbPath, "archive", "packages");

    if (!Files.exists(archivePackagesDir)) {
      log.warn("Archive packages directory not found: {}", archivePackagesDir);
      return packageIds;
    }

    globalIndexDb.forEach(
        (key, value) -> {
          GlobalIndexKey globalIndexKey = GlobalIndexKey.deserialize(key);
          if (globalIndexKey instanceof PackageKey) {
            GlobalIndexValue globalIndexValue = GlobalIndexValue.deserialize(value);
            PackageValue packageValue = ((PackageValue) globalIndexValue);
            if (!packageValue.isTemp()) { // exclude temp

              packageIds.add(((PackageKey) globalIndexKey).getPackageId());
            }
          }
        });

    log.info("Found total package ids {}", packageIds.size());
    return packageIds;
  }

  public Map<String, ArchiveFileLocation> getPackageHashOffsetMappings(int packageId) {
    Map<String, ArchiveFileLocation> packagesHashOffset = new HashMap<>();

    // Get archive packages directory path
    Path archivePackagesDir = Paths.get(dbPath, "archive", "packages");

    if (!Files.exists(archivePackagesDir)) {
      log.warn("Archive packages directory not found: {}", archivePackagesDir);
      return packagesHashOffset;
    }

    globalIndexDb.forEach(
        (key, value) -> {
          GlobalIndexKey globalIndexKey = GlobalIndexKey.deserialize(key);
          if (globalIndexKey instanceof PackageKey) {
            PackageKey packageKey = (PackageKey) globalIndexKey;
            if (packageKey.getPackageId() == packageId) {
              GlobalIndexValue globalIndexValue = GlobalIndexValue.deserialize(value);
              PackageValue packageValue = ((PackageValue) globalIndexValue);
              log.info(packageValue.toString());
              if (!packageValue.isTemp()) { // exclude temp

                for (FirstBlock firstBlock : packageValue.getFirstblocks()) {

                  int index = packageKey.getPackageId();
                  String archFolder = String.format("arch%04d", (index / 100000));

                  if (firstBlock.getWorkchain() == -1) {

                    String indexStr = String.format("%05d", index);
                    String absoluteArchFolder = archivePackagesDir.resolve(archFolder).toString();
                    String absoluteIndexFolder =
                        Paths.get(absoluteArchFolder, "archive." + indexStr + ".index").toString();

                    try (ArchiveIndexReader indexReader =
                        new ArchiveIndexReader(absoluteIndexFolder)) {
                      Map<String, Long> hashOffsetMap = indexReader.getAllHashOffsetMappings();

                      for (Map.Entry<String, Long> entry : hashOffsetMap.entrySet()) {
                        String k = entry.getKey();
                        long v = entry.getValue();

                        ArchiveFileLocation location =
                            ArchiveFileLocation.create("", absoluteIndexFolder, k, packageId, v);
                        packagesHashOffset.put(k, location);
                      }
                    } catch (IOException e) {
                      throw new RuntimeException(e);
                    }
                  } else {

                  }
                }
              }
            }
          }
        });
    return packagesHashOffset;
  }

  /**
   * Reads hash-&gt;offset mappings from a specific archive index database.
   *
   * @param packagePath Path to the package file
   * @param indexPath Path to the index database
   * @param packageId Package ID
   * @return Map of file hash to ArchiveFileLocation
   */
  private Map<String, ArchiveFileLocation> readArchiveIndexDatabase(
      String packagePath, String indexPath, int packageId) {
    Map<String, ArchiveFileLocation> fileLocations = new HashMap<>();

    try (ArchiveIndexReader indexReader = new ArchiveIndexReader(indexPath)) { // haja
      Map<String, Long> hashOffsetMap = indexReader.getAllHashOffsetMappings();

      for (Map.Entry<String, Long> entry : hashOffsetMap.entrySet()) {
        String hash = entry.getKey();
        long offset = entry.getValue();

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
                                    new ArchiveIndexReader(indexPath.toString())) { // haja
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
  public String getArchivePackagesByDirScan(long packageId) {
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
          .filter(Objects::nonNull)
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

  /**
   * Gets the archive index (package ID) for a given workchain and seqno. This optimized version
   * uses the pre-built in-memory index for O(log n) lookup instead of iterating through all
   * packages (O(n)).
   *
   * @param wc Workchain ID
   * @param seqno Sequence number
   * @return Package ID that contains the block, or 0 if not found
   */
  public int getArchiveIndexBySeqno(long wc, long seqno) {
    // Get the TreeMap for this workchain
    TreeMap<Integer, Integer> seqnoMap = packageIndexByWorkchain.get((int) wc);

    if (seqnoMap == null || seqnoMap.isEmpty()) {
      log.debug("No packages found for workchain {}", wc);
      return 0;
    }

    // Find the floor entry: largest seqno <= target seqno
    // This gives us the package that starts at or before our target seqno
    Map.Entry<Integer, Integer> floorEntry = seqnoMap.floorEntry((int) seqno);

    if (floorEntry == null) {
      log.debug("No package found for workchain {} and seqno {} (seqno too small)", wc, seqno);
      return 0;
    }

    // Return the package ID
    int packageId = floorEntry.getValue();
    log.debug(
        "Found package {} for workchain {} seqno {} (package starts at seqno {})",
        packageId,
        wc,
        seqno,
        floorEntry.getKey());

    return packageId;
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

    if (globalIndexDb != null) {
      globalIndexDb.close();
    }
  }
}
