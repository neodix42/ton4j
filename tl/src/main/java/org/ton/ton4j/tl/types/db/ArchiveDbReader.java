package org.ton.ton4j.tl.types.db;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.ByteReader;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.tlb.BlockHandle;
import org.ton.ton4j.tlb.BlockInfo;
import org.ton.ton4j.tlb.ShardIdent;

/** Specialized reader for TON archive database. */
@Slf4j
public class ArchiveDbReader implements Closeable {

  private final String dbPath;
  private final Map<String, RocksDbWrapper> indexDbs = new HashMap<>();
  private final Map<String, PackageReader> packageReaders = new HashMap<>();
  private final Map<String, ArchiveInfo> archiveInfos = new HashMap<>();
  private RocksDbWrapper globalIndexDb;
  private final Map<String, PackageReader> filesPackageReaders = new HashMap<>();

  /**
   * Creates a new ArchiveDbReader.
   *
   * @param dbPath Path to the archive database directory
   * @throws IOException If an I/O error occurs
   */
  public ArchiveDbReader(String dbPath) throws IOException {
    this.dbPath = dbPath;

    // Discover all archive folders
    discoverArchives();

    // Initialize Files database global index
    initializeFilesDatabase();
  }

  /**
   * Discovers all archive folders and their associated index and package files. Also discovers
   * packages referenced in the Files database global index.
   *
   * @throws IOException If an I/O error occurs
   */
  private void discoverArchives() throws IOException {
    // First, discover traditional archive packages
    discoverTraditionalArchives();

    // Then, discover packages from Files database global index
    discoverFilesArchives();
  }

  /** Discovers traditional archive folders in the packages directory. */
  private void discoverTraditionalArchives() throws IOException {
    Path packagesPath = Paths.get(dbPath, "packages");
    if (!Files.exists(packagesPath)) {
      log.warn("Packages directory not found: {}", packagesPath);
      return;
    }

    // Find all archive folders (arch0000, arch0001, etc.) and key folders (key000, key001, etc.)
    Pattern archPattern = Pattern.compile("arch(\\d+)");
    Pattern keyPattern = Pattern.compile("key(\\d+)");

    Files.list(packagesPath)
        .filter(Files::isDirectory)
        .forEach(
            archDir -> {
              String dirName = archDir.getFileName().toString();
              Matcher archMatcher = archPattern.matcher(dirName);
              Matcher keyMatcher = keyPattern.matcher(dirName);

              if (archMatcher.matches() || keyMatcher.matches()) {
                try {
                  // Get the archive ID from whichever matcher matched
                  int archiveId =
                      archMatcher.matches()
                          ? Integer.parseInt(archMatcher.group(1))
                          : Integer.parseInt(keyMatcher.group(1));

                  // Find all index files in this directory
                  List<Path> indexFiles =
                      Files.list(archDir)
                          .filter(path -> path.toString().endsWith(".index"))
                          .collect(Collectors.toList());

                  // Find all package files in this directory
                  List<Path> packageFiles =
                      Files.list(archDir)
                          .filter(path -> path.toString().endsWith(".pack"))
                          .collect(Collectors.toList());

                  // Create archive info
                  for (Path indexFile : indexFiles) {
                    String indexName = indexFile.getFileName().toString();
                    String baseName = indexName.substring(0, indexName.lastIndexOf('.'));

                    // Find ALL matching package files for this index
                    List<Path> matchingPackageFiles =
                        packageFiles.stream()
                            .filter(
                                path ->
                                    path.getFileName().toString().startsWith(baseName)
                                        && path.getFileName().toString().endsWith(".pack"))
                            .collect(Collectors.toList());

                    // Create separate archive entries for each package file
                    for (Path packageFile : matchingPackageFiles) {
                      String packageName = packageFile.getFileName().toString();
                      // Remove .pack extension to get the full package identifier
                      String packageBaseName =
                          packageName.substring(0, packageName.lastIndexOf('.'));
                      String archiveKey = dirName + "/" + packageBaseName;

                      archiveInfos.put(
                          archiveKey,
                          new ArchiveInfo(archiveId, indexFile.toString(), packageFile.toString()));

                      log.info(
                          "Discovered traditional archive: "
                              + archiveKey
                              + " (index: "
                              + indexFile
                              + ", package: "
                              + packageFile
                              + ")");
                    }
                  }
                } catch (IOException e) {
                  log.error("Error discovering archives in {}:{}", archDir, e.getMessage());
                }
              }
            });
  }

  /**
   * Discovers packages referenced in the Files database global index. This includes both Files
   * database packages and traditional archive packages without .index files.
   */
  private void discoverFilesArchives() throws IOException {
    // Initialize Files database first if not already done
    if (globalIndexDb == null) {
      initializeFilesDatabase();
    }

    if (globalIndexDb == null) {
      return; // No Files database available
    }

    // Discover packages that are indexed in the Files database global index
    // This includes both Files database packages and traditional archive packages without .index
    // files
    Map<String, String> discoveredPackages = new HashMap<>();

    globalIndexDb.forEach(
        (key, value) -> {
          try {
            String hash = new String(key);
            if (!isValidHexString(hash)) {
              return; // Skip non-hex keys
            }

            // The value format varies, but we need to extract package information
            // Try to parse as package reference
            if (value.length >= 4) {
              // This could be a reference to a traditional archive package
              // The value might contain package name or path information
              String valueStr = new String(value);

              // Check if this references a traditional archive package
              if (valueStr.contains("archive.")) {
                // Extract package name from the value
                String packageName = extractPackageNameFromValue(valueStr);
                if (packageName != null) {
                  // Find the actual package file
                  Path packagePath = findArchivePackage(packageName);
                  if (packagePath != null && Files.exists(packagePath)) {
                    discoveredPackages.put(packageName, packagePath.toString());
                  }
                }
              } else {
                // This might be a Files database package reference
                try {
                  if (value.length >= 16) { // At least 8 bytes for package_id + 8 bytes for offset
                    ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
                    long packageId = buffer.getLong();

                    // Construct Files database package file name
                    String packageFileName = String.format("%010d.pack", packageId);
                    Path filesPackagesPath = Paths.get(dbPath, "..", "files", "packages");
                    Path packagePath = filesPackagesPath.resolve(packageFileName);

                    if (Files.exists(packagePath)) {
                      discoveredPackages.put(packageFileName, packagePath.toString());
                    }
                  }
                } catch (Exception e) {
                  // Silently skip if not a Files database reference
                }
              }
            }
          } catch (Exception e) {
            // Silently skip errors for individual entries
          }
        });

    // Also discover traditional archive packages that don't have .index files
    discoverOrphanedArchivePackages(discoveredPackages);

    // Create archive info entries for discovered packages
    for (Map.Entry<String, String> entry : discoveredPackages.entrySet()) {
      String packageFileName = entry.getKey();
      String packagePath = entry.getValue();

      // Extract package ID from filename (remove .pack extension)
      String packageBaseName = packageFileName.substring(0, packageFileName.lastIndexOf('.'));
      String archiveKey;

      if (packagePath.contains("/files/packages/")) {
        archiveKey = "files/" + packageBaseName;
      } else {
        // This is a traditional archive package without .index file
        archiveKey = "orphaned/" + packageBaseName;
      }

      // For Files database packages, we don't have separate index files
      // The global index serves as the index for all packages
      archiveInfos.put(
          archiveKey,
          new ArchiveInfo(-1, null, packagePath)); // Use -1 to indicate Files database package

      log.info("Discovered Files-indexed archive: {} (package: {})", archiveKey, packagePath);
    }
  }

  /**
   * Discovers traditional archive packages that don't have corresponding .index files. These
   * packages should be accessible through the Files database global index.
   */
  private void discoverOrphanedArchivePackages(Map<String, String> discoveredPackages)
      throws IOException {
    Path packagesPath = Paths.get(dbPath, "packages");
    if (!Files.exists(packagesPath)) {
      return;
    }

    // Find all archive folders (arch0000, arch0001, etc.)
    Pattern archPattern = Pattern.compile("arch(\\d+)");

    Files.list(packagesPath)
        .filter(Files::isDirectory)
        .forEach(
            archDir -> {
              String dirName = archDir.getFileName().toString();
              Matcher archMatcher = archPattern.matcher(dirName);

              if (archMatcher.matches()) {
                try {
                  // Find all package files in this directory
                  List<Path> packageFiles =
                      Files.list(archDir)
                          .filter(path -> path.toString().endsWith(".pack"))
                          .collect(Collectors.toList());

                  for (Path packageFile : packageFiles) {
                    String packageName = packageFile.getFileName().toString();
                    String baseName = packageName.substring(0, packageName.lastIndexOf('.'));

                    // Check if this package has a corresponding .index file
                    Path indexFile = archDir.resolve(baseName + ".index");
                    if (!Files.exists(indexFile)) {
                      // Also check if this package was already discovered as a traditional archive
                      // by checking if any existing archive info uses this package path
                      boolean alreadyDiscovered =
                          archiveInfos.values().stream()
                              .anyMatch(info -> packageFile.toString().equals(info.packagePath));

                      if (!alreadyDiscovered) {
                        // Additional check: see if there's a traditional archive that covers the
                        // same content
                        // This prevents reading the same blocks from multiple sources
                        boolean hasRelatedTraditionalArchive =
                            isPackageCoveredByTraditionalArchive(packageName, dirName);

                        if (!hasRelatedTraditionalArchive) {
                          // This is truly an orphaned package - add it to discovered packages
                          discoveredPackages.put(packageName, packageFile.toString());
                          log.info(
                              "Found orphaned archive package: {} (no .index file)", packageFile);
                        } else {
                          log.debug(
                              "Skipping orphaned package {} - already covered by traditional archive",
                              packageFile);
                        }
                      }
                    }
                  }
                } catch (IOException e) {
                  log.error(
                      "Error discovering orphaned packages in {}: {}", archDir, e.getMessage());
                }
              }
            });
  }

  /**
   * Checks if an orphaned package is already covered by a traditional archive. This prevents
   * reading the same blocks from multiple sources.
   */
  private boolean isPackageCoveredByTraditionalArchive(String packageName, String dirName) {
    // Extract the base name from the package (e.g., "archive.00479.0:8000000000000000.pack" ->
    // "archive.00479")
    String baseName = packageName.substring(0, packageName.lastIndexOf('.'));

    // Handle shard-specific packages using regex to match any shard pattern
    // Pattern: archive.XXXXX.workchain:shard or archive.XXXXX:shard
    // Examples: "archive.00479.0:8000000000000000", "archive.00479:-1:8000000000000000", etc.
    Pattern shardPattern = Pattern.compile("^(.+?)(?:\\.[^:]*)?:([0-9a-fA-F]+)$");
    Matcher matcher = shardPattern.matcher(baseName);

    String baseArchiveName;
    String shardId = null;

    if (matcher.matches()) {
      // This is a shard-specific package
      baseArchiveName = matcher.group(1);
      shardId = matcher.group(2);
      log.debug(
          "Detected shard-specific package: {} -> base: {}, shard: {}",
          packageName,
          baseArchiveName,
          shardId);
    } else {
      // This is not a shard-specific package
      baseArchiveName = baseName;
    }

    // Check if there's a traditional archive with this base name
    String traditionalArchiveKey = dirName + "/" + baseArchiveName;
    boolean hasTraditional = archiveInfos.containsKey(traditionalArchiveKey);

    // If we found a shard-specific package, also check for the exact shard-specific traditional
    // archive
    boolean hasShardSpecific = false;
    if (shardId != null) {
      String shardSpecificKey = dirName + "/" + baseName;
      hasShardSpecific = archiveInfos.containsKey(shardSpecificKey);
    }

    // Also check for any traditional archives that start with the base name and contain shard info
    boolean hasAnyShardVariant =
        archiveInfos.keySet().stream()
            .anyMatch(key -> key.startsWith(dirName + "/" + baseArchiveName) && key.contains(":"));

    if (hasTraditional || hasShardSpecific || hasAnyShardVariant) {
      log.debug(
          "Package {} is covered by traditional archive. Found: traditional={}, shardSpecific={}, anyShardVariant={}",
          packageName,
          hasTraditional,
          hasShardSpecific,
          hasAnyShardVariant);
      return true;
    }

    return false;
  }

  /** Extracts package name from a Files database value string. */
  private String extractPackageNameFromValue(String valueStr) {
    // This is a simplified implementation - the actual format may vary
    // Look for patterns like "archive.00100" in the value
    Pattern pattern = Pattern.compile("archive\\.(\\d+)(?:\\.[^\\s]*)?");
    Matcher matcher = pattern.matcher(valueStr);
    if (matcher.find()) {
      return matcher.group(0) + ".pack";
    }
    return null;
  }

  /** Finds the actual path to an archive package by name. */
  private Path findArchivePackage(String packageName) {
    try {
      Path packagesPath = Paths.get(dbPath, "packages");
      if (!Files.exists(packagesPath)) {
        return null;
      }

      // Search in all archive directories
      Pattern archPattern = Pattern.compile("arch(\\d+)");

      return Files.list(packagesPath)
          .filter(Files::isDirectory)
          .filter(archDir -> archPattern.matcher(archDir.getFileName().toString()).matches())
          .map(archDir -> archDir.resolve(packageName))
          .filter(Files::exists)
          .findFirst()
          .orElse(null);
    } catch (IOException e) {
      return null;
    }
  }

  /** Initializes the Files database global index. */
  private void initializeFilesDatabase() {
    // Check if files database exists
    Path filesPath = Paths.get(dbPath, "..", "files");
    Path globalIndexPath = filesPath.resolve("globalindex");

    if (Files.exists(globalIndexPath)) {
      try {
        globalIndexDb = new RocksDbWrapper(globalIndexPath.toString());
        log.info("Initialized Files database global index: {}", globalIndexPath);
      } catch (IOException e) {
        log.warn("Could not initialize Files database global index: {}", e.getMessage());
      }
    } else {
      log.info("Files database global index not found at: {}", globalIndexPath);
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

  /**
   * Reads a block by its hash.
   *
   * @param hash The block hash
   * @return The block data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public byte[] readBlock(String hash) throws IOException {
    log.info("Looking for block with hash: {}", hash);

    // Try to find the block in any archive
    for (Map.Entry<String, ArchiveInfo> archiveEntry : archiveInfos.entrySet()) {
      String archiveKey = archiveEntry.getKey();
      ArchiveInfo archiveInfo = archiveEntry.getValue();

      log.debug("Checking archive: {}", archiveKey);

      try {
        // Check if this is a Files database package (indicated by null indexPath)
        if (archiveInfo.indexPath == null) {
          // This is a Files database package, use global index
          log.debug("Checking Files database package: {}", archiveKey);
          byte[] data = readBlockFromFilesPackage(hash, archiveKey, archiveInfo);
          if (data != null) {
            log.info("Found block {} in Files database package: {}", hash, archiveKey);
            return data;
          }
        } else {
          // This is a traditional archive package with its own index
          log.debug("Checking traditional archive package: {}", archiveKey);
          byte[] data = readBlockFromTraditionalArchive(hash, archiveKey, archiveInfo);
          if (data != null) {
            log.info("Found block {} in traditional archive: {}", hash, archiveKey);
            return data;
          }
        }
      } catch (IOException e) {
        log.warn("Error reading block {} from archive {}: {}", hash, archiveKey, e.getMessage());
      }
    }

    // Also try to read directly from Files database
    if (globalIndexDb != null) {
      log.debug("Checking Files database directly for hash: {}", hash);
      byte[] data = readBlockFromFilesDatabase(hash);
      if (data != null) {
        log.info("Found block {} in Files database", hash);
        return data;
      }
    }

    // Block not found in any archive
    log.warn("Block {} not found in any archive", hash);
    return null;
  }

  /** Reads a block from a traditional archive package. */
  private byte[] readBlockFromTraditionalArchive(
      String hash, String archiveKey, ArchiveInfo archiveInfo) throws IOException {
    // Get the index DB
    RocksDbWrapper indexDb = getIndexDb(archiveKey, archiveInfo.indexPath);

    // Try to get the offset from this index
    byte[] offsetBytes = indexDb.get(hash.getBytes());
    if (offsetBytes != null) {
      // Convert the offset to a long
      long offset;
      try {
        // Try to parse the offset as a string first (as in C++ implementation)
        String offsetStr = new String(offsetBytes);
        try {
          offset = Long.parseLong(offsetStr.trim());
        } catch (NumberFormatException e) {
          // If string parsing fails, try binary format as fallback
          if (offsetBytes.length >= 8) {
            offset = ByteBuffer.wrap(offsetBytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
          } else {
            return null;
          }
        }
      } catch (Exception e) {
        log.warn(
            "Error parsing offset for key {} from archive {}: {}",
            hash,
            archiveKey,
            e.getMessage());
        return null;
      }

      // Validate the offset
      if (offset < 0) {
        log.warn("Negative seek offset {} for key {} in archive {}", offset, hash, archiveKey);
        return null;
      }

      // Get the package reader
      PackageReader packageReader = getPackageReader(archiveKey, archiveInfo.packagePath);

      // Get the entry at the offset
      PackageReader.PackageEntry entry = packageReader.getEntryAt(offset);

      return entry.getData();
    }

    return null;
  }

  /** Reads a block from a Files database package using the global index. */
  private byte[] readBlockFromFilesPackage(String hash, String archiveKey, ArchiveInfo archiveInfo)
      throws IOException {
    // Check if this is an orphaned package (no index file)
    if (archiveKey.startsWith("orphaned/")) {
      // Read directly from the package file like readFromOrphanedPackage does
      return readBlockFromOrphanedPackage(hash, archiveKey, archiveInfo);
    }

    if (globalIndexDb == null) {
      return null;
    }

    // Extract package filename from the archive key (e.g., "files/0000000100" -> "0000000100.pack")
    String packageBaseName = archiveKey.substring(archiveKey.lastIndexOf('/') + 1);
    String packageFileName = packageBaseName + ".pack";

    // Try to get the offset from the global index
    byte[] offsetBytes = globalIndexDb.get(hash.getBytes());
    if (offsetBytes != null) {
      try {
        // Parse the value to get package location info
        if (offsetBytes.length >= 16) { // At least 8 bytes for package_id + 8 bytes for offset
          ByteBuffer buffer = ByteBuffer.wrap(offsetBytes).order(ByteOrder.LITTLE_ENDIAN);
          long packageId = buffer.getLong();
          long offset = buffer.getLong();

          // Check if this entry belongs to the current package
          String entryPackageFileName = String.format("%010d.pack", packageId);
          if (!entryPackageFileName.equals(packageFileName)) {
            return null; // This entry belongs to a different package
          }

          // Validate the offset
          if (offset < 0) {
            log.warn(
                "Negative seek offset {} for key {} in Files package {}",
                offset,
                hash,
                packageFileName);
            return null;
          }

          // Get the package reader
          PackageReader packageReader =
              getFilesPackageReader(packageFileName, archiveInfo.packagePath);

          // Get the entry at the offset
          PackageReader.PackageEntry entry = packageReader.getEntryAt(offset);

          return entry.getData();
        }
      } catch (Exception e) {
        log.warn(
            "Error reading block {} from Files package {}: {}",
            hash,
            packageFileName,
            e.getMessage());
      }
    }

    return null;
  }

  /**
   * Reads a block directly from an orphaned package file (no index file available). This method
   * reads the package file sequentially to find the specific block.
   */
  private byte[] readBlockFromOrphanedPackage(
      String hash, String archiveKey, ArchiveInfo archiveInfo) throws IOException {
    try {
      // Read the entire package file
      byte[] packageData = Files.readAllBytes(Paths.get(archiveInfo.packagePath));
      ByteReader reader = new ByteReader(packageData);

      // Read package header
      int packageHeaderMagic = reader.readIntLittleEndian();
      if (packageHeaderMagic != 0xae8fdd01) {
        log.warn(
            "Invalid package header magic in {}: expected 0xae8fdd01, got 0x{}",
            archiveInfo.packagePath,
            Integer.toHexString(packageHeaderMagic));
        return null;
      }

      // Read all entries in the package to find the target hash
      while (reader.getDataSize() > 0) {
        try {
          // Read entry header
          short entryHeaderMagic = reader.readShortLittleEndian();
          if (entryHeaderMagic != (short) 0x1e8b) {
            log.warn(
                "Invalid entry header magic in {}: expected 0x1e8b, got 0x{}",
                archiveInfo.packagePath,
                Integer.toHexString(entryHeaderMagic & 0xFFFF));
            break;
          }

          int filenameLength = reader.readShortLittleEndian();
          int bocSize = reader.readIntLittleEndian();

          // Read filename
          int[] filenameInts = reader.readBytes(filenameLength);
          byte[] filenameBytes = new byte[filenameInts.length];
          for (int i = 0; i < filenameInts.length; i++) {
            filenameBytes[i] = (byte) filenameInts[i];
          }
          String filename = new String(filenameBytes);

          // Read BOC data
          int[] bocInts = reader.readBytes(bocSize);
          byte[] bocData = new byte[bocInts.length];
          for (int i = 0; i < bocInts.length; i++) {
            bocData[i] = (byte) bocInts[i];
          }

          // Extract hash from filename and check if it matches
          String entryHash = extractHashFromFilename(filename);
          if (entryHash != null && entryHash.equalsIgnoreCase(hash)) {
            return bocData;
          }

        } catch (Exception e) {
          log.warn(
              "Error reading entry from orphaned package {}: {}",
              archiveInfo.packagePath,
              e.getMessage());
          break;
        }
      }

    } catch (IOException e) {
      log.error("Error reading orphaned package {}: {}", archiveInfo.packagePath, e.getMessage());
    }

    return null;
  }

  /**
   * Reads a block info by its hash.
   *
   * @param hash The block hash
   * @return The block info, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public BlockInfo readBlockInfo(String hash) throws IOException {
    byte[] data = readBlock(hash);
    if (data == null) {
      return null;
    }

    ByteReader reader = new ByteReader(data);
    byte[] magicBytes = new byte[4];
    System.arraycopy(data, 0, magicBytes, 0, 4);
    String magicId = bytesToHex(magicBytes);

    if (magicId.equals("27e7c64a")) { // db.block.info#4ac6e727
      ByteReader valueReader = new ByteReader(data);
      int[] bytesArray = valueReader.readBytes();
      byte[] signedBytes = new byte[bytesArray.length];
      for (int i = 0; i < bytesArray.length; i++) {
        signedBytes[i] = (byte) bytesArray[i];
      }
      ByteBuffer buffer = ByteBuffer.wrap(signedBytes);
      //      return BlockInfo.deserialize(buffer); // todo
      return null;
    } else {
      throw new IOException("Invalid block info magic: " + magicId);
    }
  }

  public List<Block> getAllBlocks() throws IOException {
    List<Block> blocks = new ArrayList<>();
    Map<String, byte[]> entries = getAllEntries();
    for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
      try {
        //        log.info("key " + entry.getKey());
        Cell c = CellBuilder.beginCell().fromBoc(entry.getValue()).endCell();
        long magic = c.getBits().preReadUint(32).longValue();
        if (magic == 0x11ef55aaL) { // block
          Block block = Block.deserialize(CellSlice.beginParse(c));
          blocks.add(block);
        } else {
          //          log.info("not a block");
          // return Block.builder().build();
        }
      } catch (Throwable e) {
        log.error("Error parsing block {}", e.getMessage());
        // return Block.builder().build();
      }
    }
    return blocks;
  }

  public Map<String, Block> getAllBlocksWithHashes() throws IOException {
    Map<String, Block> blocks = new HashMap<>();
    Map<String, byte[]> entries = getAllEntries();
    for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
      try {
        //        log.info("key " + entry.getKey());
        Cell c = CellBuilder.beginCell().fromBoc(entry.getValue()).endCell();
        long magic = c.getBits().preReadUint(32).longValue();
        if (magic == 0x11ef55aaL) { // block
          Block block = Block.deserialize(CellSlice.beginParse(c));
          blocks.put(entry.getKey(), block);
        } else {
          //          log.info("not a block");
          // return Block.builder().build();
        }
      } catch (Throwable e) {
        log.error("Error parsing block {}", e.getMessage());
        // return Block.builder().build();
      }
    }
    return blocks;
  }

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
        if (archiveInfo.indexPath == null) {
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

  /** Reads blocks from a traditional archive package with its own index. */
  private void readFromTraditionalArchive(
      String archiveKey, ArchiveInfo archiveInfo, Map<String, byte[]> blocks) throws IOException {
    // Get the index DB
    RocksDbWrapper indexDb = getIndexDb(archiveKey, archiveInfo.indexPath);

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
              PackageReader packageReader = getPackageReader(archiveKey, archiveInfo.packagePath);

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
  private void readFromFilesPackage(
      String archiveKey, ArchiveInfo archiveInfo, Map<String, byte[]> blocks) {
    // Check if this is an orphaned package (no index file)
    if (archiveKey.startsWith("orphaned/")) {
      // Read directly from the package file like TestFilesDbReader does
      readFromOrphanedPackage(archiveKey, archiveInfo, blocks);
      return;
    }

    if (globalIndexDb == null) {
      return;
    }

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
                PackageReader packageReader =
                    getFilesPackageReader(packageFileName, archiveInfo.packagePath);
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
      //      log.info("Reading orphaned package directly: {}", archiveInfo.packagePath);

      // Read the entire package file
      byte[] packageData = Files.readAllBytes(Paths.get(archiveInfo.packagePath));
      ByteReader reader = new ByteReader(packageData);

      // Read package header
      int packageHeaderMagic = reader.readIntLittleEndian();
      if (packageHeaderMagic != 0xae8fdd01) {
        log.warn(
            "Invalid package header magic in {}: expected 0xae8fdd01, got 0x{}",
            archiveInfo.packagePath,
            Integer.toHexString(packageHeaderMagic));
        return;
      }

      int entryCount = 0;
      // Read all entries in the package
      while (reader.getDataSize() > 0) {
        try {
          // Read entry header
          short entryHeaderMagic = reader.readShortLittleEndian();
          if (entryHeaderMagic != (short) 0x1e8b) {
            log.warn(
                "Invalid entry header magic in {}: expected 0x1e8b, got 0x{}",
                archiveInfo.packagePath,
                Integer.toHexString(entryHeaderMagic & 0xFFFF));
            break;
          }

          int filenameLength = reader.readShortLittleEndian();
          int bocSize = reader.readIntLittleEndian();

          // Read filename
          int[] filenameInts = reader.readBytes(filenameLength);
          byte[] filenameBytes = new byte[filenameInts.length];
          for (int i = 0; i < filenameInts.length; i++) {
            filenameBytes[i] = (byte) filenameInts[i];
          }
          String filename = new String(filenameBytes);

          // Read BOC data
          int[] bocInts = reader.readBytes(bocSize);
          byte[] bocData = new byte[bocInts.length];
          for (int i = 0; i < bocInts.length; i++) {
            bocData[i] = (byte) bocInts[i];
          }

          entryCount++;

          // Extract hash from filename if it's a block or proof
          String hash = extractHashFromFilename(filename);
          if (hash != null) {
            blocks.put(hash, bocData);

            //            // Log first few and last few entries for verification
            //            if (entryCount <= 5 || entryCount % 50 == 0) {
            //              log.info(
            //                  "Entry {}: filename={}, bocSize={}, hash={}",
            //                  entryCount,
            //                  filename,
            //                  bocSize,
            //                  hash);
            //            }
          }

        } catch (Exception e) {
          log.warn(
              "Error reading entry {} from orphaned package {}: {}",
              entryCount,
              archiveInfo.packagePath,
              e.getMessage());
          break;
        }
      }

      log.info(
          "Successfully read {} entries from orphaned package: {}",
          entryCount,
          archiveInfo.packagePath);

    } catch (IOException e) {
      log.error("Error reading orphaned package {}: {}", archiveInfo.packagePath, e.getMessage());
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
    if (globalIndexDb == null) {
      return null;
    }

    try {
      // Try to get the offset from the global index
      byte[] offsetBytes = globalIndexDb.get(hash.getBytes());
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
          Path filesPackagesPath = Paths.get(dbPath, "..", "files", "packages");
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

  /**
   * Reads blocks from the Files database using the global index.
   *
   * @param blocks Map to add the blocks to
   */
  private void readFromFilesDatabase(Map<String, byte[]> blocks) {
    try {
      Path filesPackagesPath = Paths.get(dbPath, "..", "files", "packages");

      globalIndexDb.forEach(
          (key, value) -> {
            try {
              String hash = new String(key);
              if (!isValidHexString(hash)) {
                return; // Skip non-hex keys
              }

              // Parse the value to get package location info
              // According to the analysis, this should contain package_id, offset, size
              if (value.length >= 16) { // At least 8 bytes for package_id + 8 bytes for offset
                ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
                long packageId = buffer.getLong();
                long offset = buffer.getLong();
                // size might be in the remaining bytes, but we can read until end of entry

                // Construct package file path
                String packageFileName = String.format("%010d.pack", packageId);
                Path packagePath = filesPackagesPath.resolve(packageFileName);

                if (Files.exists(packagePath)) {
                  try {
                    PackageReader packageReader =
                        getFilesPackageReader(packageFileName, packagePath.toString());
                    PackageReader.PackageEntry entry = packageReader.getEntryAt(offset);

                    if (entry != null) {
                      blocks.put(hash, entry.getData());
                    }
                  } catch (IOException e) {
                    // Silently skip errors for individual entries
                  }
                }
              }
            } catch (Exception e) {
              // Silently skip errors for individual entries
            }
          });
    } catch (Exception e) {
      log.warn("Error reading from Files database: {}", e.getMessage());
    }
  }

  /** Extracts block ID from a TL key. */
  private String extractBlockIdFromKey(byte[] key) {
    if (key == null || key.length < 4) {
      return null;
    }

    try {

      // Method 2: Try to parse as TL-serialized BlockIdExt
      if (key.length >= 80) { // BlockIdExt serialized size
        ByteBuffer buffer = ByteBuffer.wrap(key);
        try {
          org.ton.ton4j.tl.liteserver.responses.BlockIdExt blockIdExt =
              org.ton.ton4j.tl.liteserver.responses.BlockIdExt.deserialize(buffer);

          // Create a meaningful block ID string from BlockIdExt components
          return String.format(
              "(%d,%s,%d):%s:%s",
              blockIdExt.getWorkchain(),
              Long.toUnsignedString(blockIdExt.shard, 16),
              blockIdExt.getSeqno(),
              blockIdExt.getRootHash(),
              blockIdExt.getFileHash());
        } catch (Exception e) {
          // Fall through to other methods
        }
      }

      // Method 1: Try to parse as hex string (most common case)
      String keyStr = new String(key);
      if (isValidHexString(keyStr)) {
        return keyStr;
      }

      // Method 3: Try to parse as TL key with magic number
      if (key.length >= 4) {
        ByteBuffer buffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
        int magic = buffer.getInt();

        // Check for known TL key magic numbers
        switch (magic) {
          case 0x7dc40502: // db_filedb_key_empty
            return "empty_key";
          case 0xa504033e: // db_filedb_key_blockFile
            // Try to parse the BlockIdExt that follows
            if (key.length >= 4 + 80) {
              try {
                buffer.position(4); // Skip magic
                org.ton.ton4j.tl.liteserver.responses.BlockIdExt blockIdExt =
                    org.ton.ton4j.tl.liteserver.responses.BlockIdExt.deserialize(buffer);

                return String.format(
                    "blockFile_(%d,%s,%d):%s:%s",
                    blockIdExt.getWorkchain(),
                    Long.toUnsignedString(blockIdExt.shard, 16),
                    blockIdExt.getSeqno(),
                    blockIdExt.getRootHash(),
                    blockIdExt.getFileHash());
              } catch (Exception e) {
                // Fall through
              }
            }
            return "blockFile_" + bytesToHex(key);
          default:
            return "tl_key_" + String.format("%08x", magic) + "_" + bytesToHex(key);
        }
      }

      // Method 4: Fallback to hex representation
      return bytesToHex(key);
    } catch (Exception e) {
      log.debug("Error extracting block ID from key: {}", e.getMessage());
      return bytesToHex(key);
    }
  }

  /** Gets a package reader for Files database packages. */
  private PackageReader getFilesPackageReader(String packageKey, String packagePath)
      throws IOException {
    if (!filesPackageReaders.containsKey(packageKey)) {
      filesPackageReaders.put(packageKey, new PackageReader(packagePath));
    }
    return filesPackageReaders.get(packageKey);
  }

  /**
   * Gets all BlockHandles from RocksDB index files (primary method).
   *
   * @return Map of block ID to BlockHandle
   */
  public Map<String, BlockHandle> getAllBlockHandlesFromIndex() {
    Map<String, BlockHandle> blockHandles = new HashMap<>();

    log.info("Reading BlockHandles from RocksDB index files...");

    // Iterate through all archive index databases
    for (Map.Entry<String, ArchiveInfo> entry : archiveInfos.entrySet()) {
      String archiveKey = entry.getKey();
      ArchiveInfo archiveInfo = entry.getValue();

      if (archiveInfo.indexPath != null) { // Traditional archives with index
        try {
          RocksDbWrapper indexDb = getIndexDb(archiveKey, archiveInfo.indexPath);

          // Debug: Count different key types in this index
          Map<String, Integer> keyTypeStats = new HashMap<>();
          AtomicInteger totalKeys = new AtomicInteger(0);
          AtomicInteger blockInfoKeys = new AtomicInteger(0);

          // Look for db_blockdb_key_value entries
          indexDb.forEach(
              (key, value) -> {
                try {
                  totalKeys.incrementAndGet();
                  
                  // Debug: Analyze key types
                  String keyType = analyzeKeyType(key);
                  keyTypeStats.merge(keyType, 1, Integer::sum);
                  
                  // Check if this is a block info key by examining the TL structure
                  if (isBlockInfoKey(key)) {
                    blockInfoKeys.incrementAndGet();
                    // Parse db_block_info value to extract BlockHandle
                    BlockHandle handle = parseDbBlockInfo(value);
                    if (handle != null) {
                      String blockId = extractBlockIdFromKey(key);
                      if (blockId != null) {
                        blockHandles.put(blockId, handle);
                      }
                    }
                  }
                } catch (Exception e) {
                  log.debug("Error processing key in archive {}: {}", archiveKey, e.getMessage());
                }
              });

          log.info("Archive {}: {} total keys, {} block info keys, key types: {}", 
                   archiveKey, totalKeys.get(), blockInfoKeys.get(), keyTypeStats);
          log.info("Found {} BlockHandles in archive index: {}", blockHandles.size(), archiveKey);
        } catch (IOException e) {
          log.warn("Error reading BlockHandles from archive {}: {}", archiveKey, e.getMessage());
        }
      }
    }

    log.info("Total BlockHandles found in indexes: {}", blockHandles.size());
    return blockHandles;
  }

  /**
   * Gets all BlockHandles from package files (fallback method).
   *
   * @return Map of block ID to BlockHandle
   */
  public Map<String, BlockHandle> getAllBlockHandlesFromPackages() {
    Map<String, BlockHandle> blockHandles = new HashMap<>();

    log.info("Reading BlockHandles from package files (fallback method)...");

    Map<String, byte[]> entries = getAllEntries();
    int processedCount = 0;
    int errorCount = 0;
    
    for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
      try {
        String hash = entry.getKey();
        byte[] data = entry.getValue();
        processedCount++;

        // Try to parse as BOC and check if it contains BlockHandle information
        Cell cell = CellBuilder.beginCell().fromBoc(data).endCell();
        long magic = cell.getBits().preReadUint(32).longValue();

        if (magic == 0x4ac6e727L) { // db.block.info magic
          // This is a BlockInfo entry, parse it properly using TL schema
          try {
            BlockHandle handle = parseBlockInfoFromCell(cell);
            if (handle != null) {
              blockHandles.put(hash, handle);
            }
          } catch (Exception e) {
            log.debug("Error parsing BlockInfo cell for {}: {}", hash, e.getMessage());
            errorCount++;
          }
        } else if (magic == 0x11ef55aaL) { // Block magic
          // This is a Block entry, we can extract BlockHandle from the block structure
          try {
            Block block = Block.deserialize(CellSlice.beginParse(cell));
            if (block != null && block.getBlockInfo() != null) {
              // Create BlockHandle from block information
              // In a real implementation, we'd need to calculate the actual offset and size
              // For now, use the data size as the block size
              BlockHandle handle =
                  BlockHandle.builder()
                      .offset(BigInteger.valueOf(0)) // Would need actual offset calculation
                      .size(BigInteger.valueOf(data.length))
                      .build();

              // Create a block ID from the block info
              String blockId = createBlockIdFromBlock(block);
              if (blockId != null) {
                blockHandles.put(blockId, handle);
              }
            }
          } catch (Exception e) {
            log.debug("Error parsing block for BlockHandle extraction from {}: {}", hash, e.getMessage());
            errorCount++;
            // Continue processing other entries instead of failing completely
          }
        }
      } catch (Exception e) {
        log.debug("Error processing entry {}: {}", entry.getKey(), e.getMessage());
        errorCount++;
        // Continue processing other entries
      }
    }

    log.info("Processed {} entries, {} errors, found {} BlockHandles from packages", 
             processedCount, errorCount, blockHandles.size());
    return blockHandles;
  }

  /**
   * Parses a BlockInfo cell to extract BlockHandle information. This implements proper TL parsing
   * for db.block.info structure.
   */
  private BlockHandle parseBlockInfoFromCell(Cell cell) {
    try {
      CellSlice cs = CellSlice.beginParse(cell);

      // Skip magic (already verified)
      cs.loadUint(32);

      // Parse db.block.info TL structure
      // According to TL schema: db.block.info id:tonNode.blockIdExt flags:# prev:flags.7?int256
      // next:flags.8?int256 lt:flags.13?long ts:flags.14?int state:flags.17?int256
      // masterchain_ref_seqno:flags.23?int = db.block.Info;

      // Parse BlockIdExt (workchain:int shard:long seqno:int root_hash:int256 file_hash:int256)
      int workchain = cs.loadInt(32).intValue();
      long shard = cs.loadUint(64).longValue();
      int seqno = cs.loadInt(32).intValue();
      byte[] rootHash = cs.loadBytes(32);
      byte[] fileHash = cs.loadBytes(32);

      // Parse flags
      long flags = cs.loadUint(32).longValue();

      // Parse conditional fields based on flags
      if ((flags & (1L << 7)) != 0) { // prev
        cs.loadBytes(32); // prev hash
      }

      if ((flags & (1L << 8)) != 0) { // next
        cs.loadBytes(32); // next hash
      }

      if ((flags & (1L << 13)) != 0) { // lt
        cs.loadUint(64); // logical time
      }

      if ((flags & (1L << 14)) != 0) { // ts
        cs.loadUint(32); // timestamp
      }

      if ((flags & (1L << 17)) != 0) { // state
        cs.loadBytes(32); // state hash
      }

      if ((flags & (1L << 23)) != 0) { // masterchain_ref_seqno
        cs.loadUint(32); // masterchain reference seqno
      }

      // At this point, we've parsed the main db.block.info structure
      // The BlockHandle information (offset and size) might be stored differently
      // For now, we'll create a synthetic BlockHandle based on available information

      // In a complete implementation, we'd need to understand exactly where
      // the offset and size are stored in the TL structure
      return BlockHandle.builder()
          .offset(BigInteger.valueOf(0)) // Would need to find actual offset field
          .size(BigInteger.valueOf(1024)) // Would need to find actual size field
          .build();

    } catch (Exception e) {
      log.debug("Error parsing BlockInfo cell: {}", e.getMessage());
      return null;
    }
  }

  /** Creates a block ID string from a Block object. */
  private String createBlockIdFromBlock(Block block) {
    try {
      if (block.getBlockInfo() != null && block.getBlockInfo().getShard() != null) {
        // Create a meaningful block ID from block info
        ShardIdent shard = block.getBlockInfo().getShard();
        return String.format(
            "block_(%d,%s,%d)",
            shard.getWorkchain(),
            shard.getShardPrefix().toString(16),
            block.getBlockInfo().getSeqno());
      }
    } catch (Exception e) {
      log.debug("Error creating block ID from block: {}", e.getMessage());
    }
    return null;
  }

  /**
   * Gets all BlockHandles using both index and package methods.
   *
   * @return Map of block ID to BlockHandle
   * @throws IOException If an I/O error occurs
   */
  public Map<String, BlockHandle> getAllBlockHandles() throws IOException {
    Map<String, BlockHandle> allBlockHandles = new HashMap<>();

    // Primary: Read from index files
    Map<String, BlockHandle> indexBlockHandles = getAllBlockHandlesFromIndex();
    allBlockHandles.putAll(indexBlockHandles);

    // Fallback: Read from package files for orphaned archives
    Map<String, BlockHandle> packageBlockHandles = getAllBlockHandlesFromPackages();

    // Merge, preferring index-based results
    for (Map.Entry<String, BlockHandle> entry : packageBlockHandles.entrySet()) {
      if (!allBlockHandles.containsKey(entry.getKey())) {
        allBlockHandles.put(entry.getKey(), entry.getValue());
      }
    }

    log.info("Total unique BlockHandles: {}", allBlockHandles.size());
    return allBlockHandles;
  }

  /**
   * Gets statistics about archive entry types grouped by Block, BlockProof, and BlockHandle.
   *
   * @return Map of entry type to count
   * @throws IOException If an I/O error occurs
   */
  public Map<String, Integer> getArchiveEntryTypeStatistics() throws IOException {
    Map<String, Integer> stats = new HashMap<>();
    stats.put("Block", 0);
    stats.put("BlockProof", 0);
    stats.put("BlockHandle", 0);
    stats.put("Other", 0);

    log.info("Analyzing archive entry types grouped by Block, BlockProof, and BlockHandle...");

    // Count from RocksDB indexes (more efficient and accurate)
    for (Map.Entry<String, ArchiveInfo> entry : archiveInfos.entrySet()) {
      String archiveKey = entry.getKey();
      ArchiveInfo archiveInfo = entry.getValue();

      if (archiveInfo.indexPath != null) {
        try {
          RocksDbWrapper indexDb = getIndexDb(archiveKey, archiveInfo.indexPath);
          
          AtomicInteger blockCount = new AtomicInteger(0);
          AtomicInteger blockProofCount = new AtomicInteger(0);
          AtomicInteger blockHandleCount = new AtomicInteger(0);
          AtomicInteger otherCount = new AtomicInteger(0);
          
          indexDb.forEach(
              (key, value) -> {
                try {
                  String entryType = classifyArchiveEntry(key, value);
                  switch (entryType) {
                    case "Block":
                      blockCount.incrementAndGet();
                      break;
                    case "BlockProof":
                      blockProofCount.incrementAndGet();
                      break;
                    case "BlockHandle":
                      blockHandleCount.incrementAndGet();
                      break;
                    default:
                      otherCount.incrementAndGet();
                      break;
                  }
                } catch (Exception e) {
                  otherCount.incrementAndGet();
                }
              });
              
          stats.merge("Block", blockCount.get(), Integer::sum);
          stats.merge("BlockProof", blockProofCount.get(), Integer::sum);
          stats.merge("BlockHandle", blockHandleCount.get(), Integer::sum);
          stats.merge("Other", otherCount.get(), Integer::sum);
          
          log.info("Archive {}: Block={}, BlockProof={}, BlockHandle={}, Other={}", 
                   archiveKey, blockCount.get(), blockProofCount.get(), 
                   blockHandleCount.get(), otherCount.get());
        } catch (IOException e) {
          log.warn("Error reading index for statistics in {}: {}", archiveKey, e.getMessage());
        }
      }
    }

    // Also count from package files for orphaned archives
    Map<String, byte[]> entries = getAllEntries();
    for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
      try {
        String entryType = detectEntryType(entry.getValue());
        switch (entryType) {
          case "Block":
            stats.merge("Block", 1, Integer::sum);
            break;
          case "BlockHandle":
            stats.merge("BlockHandle", 1, Integer::sum);
            break;
          default:
            if (entryType.contains("proof") || entryType.contains("Proof")) {
              stats.merge("BlockProof", 1, Integer::sum);
            } else {
              stats.merge("Other", 1, Integer::sum);
            }
            break;
        }
      } catch (Exception e) {
        stats.merge("Other", 1, Integer::sum);
      }
    }

    log.info("Final archive entry type statistics: {}", stats);
    return stats;
  }

  /**
   * Classifies an archive entry based on its key and value for the main entry types.
   *
   * @param key The RocksDB key
   * @param value The RocksDB value
   * @return The entry type classification
   */
  private String classifyArchiveEntry(byte[] key, byte[] value) {
    if (key == null || key.length < 4) {
      return "Other";
    }

    try {
      // Check for TL-serialized keys
      ByteBuffer buffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
      int magic = buffer.getInt();

      switch (magic) {
        case 0x7f57d173: // db_blockdb_key_value - contains BlockHandle information
          return "BlockHandle";
        case 0xa504033e: // db_filedb_key_blockFile - references to block files
          return "Block";
        case 0x50a6f90f: // db_filedb_key_proof - references to proof files
        case 0xf1e3e791: // db_filedb_key_proofLink - references to proof link files
          return "BlockProof";
        default:
          // Check if it's a hex string (likely a file hash)
          String keyStr = new String(key);
          if (isValidHexString(keyStr)) {
            // For hex string keys, we need to examine the value or try to read the actual data
            // to determine if it's a block, proof, or handle
            return classifyByValue(value);
          }
          return "Other";
      }
    } catch (Exception e) {
      return "Other";
    }
  }

  /**
   * Classifies an entry by examining its value when the key is a hex string.
   *
   * @param value The RocksDB value
   * @return The entry type classification
   */
  private String classifyByValue(byte[] value) {
    if (value == null || value.length == 0) {
      return "Other";
    }

    try {
      // If the value looks like an offset (string of digits), it's likely pointing to a block
      String valueStr = new String(value).trim();
      if (valueStr.matches("^\\d+$")) {
        return "Block"; // Most hex-key entries with offset values are blocks
      }

      // If the value is binary data, try to parse it
      if (value.length >= 4) {
        // Could be a direct BOC or TL structure
        // For now, classify based on common patterns
        return "Other";
      }
    } catch (Exception e) {
      // Ignore parsing errors
    }

    return "Other";
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

  /** Analyzes the type of a RocksDB key for debugging purposes. */
  private String analyzeKeyType(byte[] key) {
    if (key == null || key.length == 0) {
      return "null_or_empty";
    }

    try {
      String keyStr = new String(key);

      // Check for system keys
      if (keyStr.equals("status")) {
        return "status_key";
      }

      // Check if it's a valid hex string
      if (isValidHexString(keyStr)) {
        return "hex_string_" + keyStr.length() + "chars";
      }

      // Check for TL-serialized keys
      if (key.length >= 4) {
        ByteBuffer buffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
        int magic = buffer.getInt();

        switch (magic) {
          case 0x9bc7a987:
            return "db_blockdb_key_value";
          case 0x7dc40502:
            return "db_filedb_key_empty";
          case 0xa504033e:
            return "db_filedb_key_blockFile";
          case 0x4ac6e727:
            return "db_block_info";
          default:
            return "tl_key_" + String.format("%08x", magic);
        }
      }

      return "unknown_" + key.length + "bytes";
    } catch (Exception e) {
      return "parse_error";
    }
  }

  /** Parses a db_block_info TL structure to extract BlockHandle. */
  private BlockHandle parseDbBlockInfo(byte[] value) {
    if (value == null || value.length == 0) {
      return null;
    }

    try {
      // Method 1: Try to parse as string offset (common in RocksDB indexes)
      String valueStr = new String(value).trim();
      if (valueStr.matches("^\\d+$")) {
        // This is just an offset, create a synthetic BlockHandle
        long offset = Long.parseLong(valueStr);
        if (offset >= 0) {
          return BlockHandle.builder()
              .offset(BigInteger.valueOf(offset))
              .size(
                  BigInteger.valueOf(
                      1024)) // Default size, will be updated when reading actual data
              .build();
        }
      }

      // Method 2: Try to parse as binary data
      if (value.length >= 8) {
        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

        // Try different positions for offset/size pairs
        for (int pos = 0; pos <= value.length - 16; pos += 4) {
          try {
            buffer.position(pos);
            long offset = buffer.getLong();
            long size = buffer.getLong();

            // Validate that this looks like a reasonable offset/size pair
            if (offset >= 0 && size > 0 && size < 100_000_000) { // Max 100MB per block
              return BlockHandle.builder()
                  .offset(BigInteger.valueOf(offset))
                  .size(BigInteger.valueOf(size))
                  .build();
            }
          } catch (Exception e) {
            // Continue trying other positions
          }
        }

        // Method 3: Try single 8-byte value as offset
        if (value.length >= 8) {
          buffer.position(0);
          long offset = buffer.getLong();
          if (offset >= 0) {
            return BlockHandle.builder()
                .offset(BigInteger.valueOf(offset))
                .size(BigInteger.valueOf(1024)) // Default size
                .build();
          }
        }
      }

      // Method 4: For very short values, try as 4-byte offset
      if (value.length >= 4) {
        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        int offset = buffer.getInt();
        if (offset >= 0) {
          return BlockHandle.builder()
              .offset(BigInteger.valueOf(offset))
              .size(BigInteger.valueOf(1024)) // Default size
              .build();
        }
      }

    } catch (Exception e) {
      log.debug("Error parsing value as BlockHandle: {}", e.getMessage());
    }

    return null;
  }

  /** Creates a synthetic BlockHandle based on package structure. */
  private BlockHandle createSyntheticBlockHandle(String hash, byte[] data) {
    // This is a fallback method that creates BlockHandle based on available information
    // In practice, the offset would be calculated from the package structure
    // and size would be the actual data size

    if (data != null && data.length > 0) {
      return BlockHandle.builder()
          .offset(BigInteger.valueOf(0)) // Would need to calculate actual offset
          .size(BigInteger.valueOf(data.length))
          .build();
    }

    return null;
  }

  /** Classifies an index entry by examining its key and value. */
  private String classifyIndexEntry(byte[] key, byte[] value) {
    if (key == null || key.length < 4) {
      return "unknown_key";
    }

    try {
      ByteBuffer buffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
      int magic = buffer.getInt();

      switch (magic) {
        case 0x7dc40502:
          return "db_filedb_key_empty";
        case 0xa504033e:
          return "db_filedb_key_blockFile";
        default:
          // Check if it's a hex string (likely a file hash)
          String keyStr = new String(key);
          if (isValidHexString(keyStr)) {
            return "file_hash_entry";
          }
          return "unknown_tl_key_" + String.format("%08x", magic);
      }
    } catch (Exception e) {
      return "parse_error";
    }
  }

  /** Detects the type of an entry by examining its BOC data. */
  private String detectEntryType(byte[] bocData) {
    if (bocData == null || bocData.length < 4) {
      return "unknown";
    }

    try {
      Cell cell = CellBuilder.beginCell().fromBoc(bocData).endCell();
      long magic = cell.getBits().preReadUint(32).longValue();

      switch ((int) magic) {
        case 0x11ef55aa:
          return "Block";
        case 0x4ac6e727:
          return "BlockHandle";
        case 0x9bc7a987:
          return "BlockInfo";
        default:
          return "unknown_" + Long.toHexString(magic);
      }
    } catch (Exception e) {
      return "parse_error";
    }
  }

  /**
   * Gets all block infos from all archives.
   *
   * @return Map of block hash to block info
   * @throws IOException If an I/O error occurs
   */
  public Map<String, BlockInfo> getAllBlockInfos() throws IOException {
    Map<String, BlockInfo> blockInfos = new HashMap<>();

    Map<String, byte[]> blocks = getAllEntries();
    for (Map.Entry<String, byte[]> entry : blocks.entrySet()) {
      String hash = entry.getKey();
      byte[] data = entry.getValue();

      Block block = getBlock(data);

      blockInfos.put(hash, block.getBlockInfo());
    }

    return blockInfos;
  }

  public static Block getBlock(byte[] data) {
    try {
      Cell c = CellBuilder.beginCell().fromBoc(data).endCell();
      long magic = c.getBits().preReadUint(32).longValue();
      if (magic == 0x11ef55aaL) { // block
        Block block = Block.deserialize(CellSlice.beginParse(c));
        //        log.info("block: {}" + block);
        return block;
      } else {
        log.info("not a block");
        return Block.builder().build();
      }
    } catch (Exception e) {
      log.warn("Error parsing block info");
      return Block.builder().build();
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
   * @return The package reader
   * @throws IOException If an I/O error occurs
   */
  private PackageReader getPackageReader(String archiveKey, String packagePath) throws IOException {
    if (!packageReaders.containsKey(archiveKey)) {
      packageReaders.put(archiveKey, new PackageReader(packagePath));
    }

    return packageReaders.get(archiveKey);
  }

  @Override
  public void close() throws IOException {
    // Close all index DBs
    for (RocksDbWrapper db : indexDbs.values()) {
      db.close();
    }

    // Close all package readers
    for (PackageReader reader : packageReaders.values()) {
      reader.close();
    }

    // Close Files database resources
    if (globalIndexDb != null) {
      globalIndexDb.close();
    }

    for (PackageReader reader : filesPackageReaders.values()) {
      reader.close();
    }
  }

  /**
   * Converts a byte array to a hexadecimal string.
   *
   * @param bytes The byte array to convert
   * @return The hexadecimal string
   */
  private static String bytesToHex(byte[] bytes) {
    StringBuilder hex = new StringBuilder(2 * bytes.length);
    for (byte b : bytes) {
      hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit((b & 0xF), 16));
    }
    return hex.toString().toLowerCase();
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

  /** Information about an archive. */
  private static class ArchiveInfo {
    private final int id;
    private final String indexPath;
    private final String packagePath;

    public ArchiveInfo(int id, String indexPath, String packagePath) {
      this.id = id;
      this.indexPath = indexPath;
      this.packagePath = packagePath;
    }
  }
}
