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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.ByteReader;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tl.types.db.files.index.IndexValue;
import org.ton.ton4j.tl.types.db.files.key.IndexKey;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.tlb.BlockHandle;
import org.ton.ton4j.tlb.ShardIdent;

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
   * @throws IOException If an I/O error occurs
   */
  public ArchiveDbReader(String rootPath) throws IOException {

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
   * found by filesystem scanning.
   */
  private void discoverArchivesFromFilesDatabase() {
    log.debug("Discovering additional archives from Files database global index...");

    // Get archive package file paths from the global index
    List<String> archivePackageFiles =
        globalIndexDbReader.getAllArchivePackageFilePathsFromFilesystem();

    int newPackages = 0;

    // Create archive info entries for discovered packages (only if not already found)
    for (String packagePath : archivePackageFiles) {
      try {
        Path path = Paths.get(packagePath);
        String fileName = path.getFileName().toString();

        // Extract package info from the file path
        Path parentDir = path.getParent();
        String dirName = parentDir.getFileName().toString();

        // Remove .pack extension to get the package base name
        String packageBaseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String archiveKey = dirName + "/" + packageBaseName;

        // Only add if not already discovered by filesystem scan
        if (!archiveInfos.containsKey(archiveKey)) {
          // Extract archive ID from directory name (arch0000 -> 0)
          int archiveId = 0;
          if (dirName.startsWith("arch")) {
            try {
              archiveId = Integer.parseInt(dirName.substring(4));
            } catch (NumberFormatException e) {
              log.debug("Could not parse archive ID from directory name: {}", dirName);
            }
          }

          // Look for corresponding index file
          String indexFileName = packageBaseName + ".index";
          Path indexPath = parentDir.resolve(indexFileName);
          String indexPathStr = Files.exists(indexPath) ? indexPath.toString() : null;

          // Create archive info
          archiveInfos.put(archiveKey, new ArchiveInfo(archiveId, indexPathStr, packagePath));
          newPackages++;

          log.debug(
              "Discovered additional archive from Files database: {} (index: {}, package: {})",
              archiveKey,
              indexPathStr != null ? indexPathStr : "none",
              packagePath);
        }

      } catch (Exception e) {
        log.debug(
            "Error processing archive package file from Files database {}: {}",
            packagePath,
            e.getMessage());
      }
    }

    if (newPackages > 0) {
      log.info("Discovered {} additional archive packages from Files database", newPackages);
    }
  }

  /** Initializes the Files database global index. */
  private void initializeFilesDatabase(String rootPath) throws IOException {
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

      //      log.debug("Checking archive: {}", archiveKey);

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
    if (globalIndexDbReader != null) {
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

    if (globalIndexDbReader == null) {
      return null;
    }

    // Extract package filename from the archive key (e.g., "files/0000000100" -> "0000000100.pack")
    String packageBaseName = archiveKey.substring(archiveKey.lastIndexOf('/') + 1);
    String packageFileName = packageBaseName + ".pack";

    // Try to get the offset from the global index
    byte[] offsetBytes = globalIndexDbReader.getGlobalIndexDb().get(hash.getBytes());
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

  public List<Block> getAllBlocks() {
    List<Block> blocks = new ArrayList<>();
    Map<String, byte[]> entries = getAllEntries();
    for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
      try {
        Cell c = CellBuilder.beginCell().fromBoc(entry.getValue()).endCell();
        long magic = c.getBits().preReadUint(32).longValue();
        if (magic == 0x11ef55aaL) { // block
          //          log.info("block");
          Block block = Block.deserialize(CellSlice.beginParse(c));
          blocks.add(block);
        } else {
          //           log.info("not a block");
        }
      } catch (Throwable e) {
        log.error("Error parsing block {}", e.getMessage());
        // return Block.builder().build();
      }
    }
    return blocks;
  }

  public Map<String, Block> getAllBlocksWithHashes() {
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
    // This includes both explicitly orphaned packages and archive packages without index files
    if (archiveKey.startsWith("orphaned/") || archiveInfo.indexPath == null) {
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
                String hash = key.toString();
                if (!isValidHexString(hash)) {
                  return; // Skip non-hex keys
                }

                // For file hash entries, the value should be raw location data (not TL objects)
                // Only hex string keys (64 chars) contain file location data
                if (hash.length() == 64) {
                  // This is a file hash entry, parse the raw location data
                  try {
                    // Use FilesDbReader's method to read the block directly
                    byte[] blockData = globalIndexDbReader.readBlock(hash);
                    if (blockData != null) {
                      blocks.put(hash, blockData);
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
      PackageReader packageReader = getPackageReader(archiveKey, archiveInfo.packagePath);

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
      //          archiveInfo.packagePath);

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

  /**
   * Reads blocks from the Files database using the global index.
   *
   * @param blocks Map to add the blocks to
   */
  private void readFromFilesDatabase(Map<String, byte[]> blocks) {
    if (globalIndexDbReader == null) {
      return;
    }

    try {
      Path filesPackagesPath = Paths.get(rootPath, "..", "files", "packages");

      globalIndexDbReader
          .getGlobalIndexDb()
          .forEach(
              (key, value) -> {
                try {
                  String hash = key.toString();
                  if (!isValidHexString(hash)) {
                    return; // Skip non-hex keys
                  }

                  // Parse the value to get package location info
                  byte[] valueBytes = value;
                  if (valueBytes.length
                      >= 16) { // At least 8 bytes for package_id + 8 bytes for offset
                    ByteBuffer buffer = ByteBuffer.wrap(valueBytes).order(ByteOrder.LITTLE_ENDIAN);
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
   * Parses a package offset value from RocksDB. Based on C++ implementation: offsets are stored as
   * strings using td::to_string(offset) and parsed back using td::to_integer<td::uint64>(value).
   *
   * @param value The RocksDB value containing the offset
   * @return BlockHandle with offset and default size, or null if parsing fails
   */
  private BlockHandle parsePackageOffset(byte[] value) {
    if (value == null || value.length == 0) {
      return null;
    }

    try {
      // C++ stores offsets as strings: td::to_string(offset)
      String offsetStr = new String(value).trim();

      // Parse as long (C++ uses td::to_integer<td::uint64>)
      long offset = Long.parseLong(offsetStr);

      if (offset >= 0) {
        return BlockHandle.builder()
            .offset(BigInteger.valueOf(offset))
            .size(
                BigInteger.valueOf(
                    1024)) // Default size, actual size determined when reading package entry
            .build();
      }
    } catch (NumberFormatException e) {
      log.debug("Failed to parse offset as string: {}", new String(value));

      // Fallback: try binary format (though C++ uses string format)
      if (value.length >= 8) {
        try {
          ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
          long offset = buffer.getLong();
          if (offset >= 0) {
            return BlockHandle.builder()
                .offset(BigInteger.valueOf(offset))
                .size(BigInteger.valueOf(1024))
                .build();
          }
        } catch (Exception e2) {
          log.debug("Failed to parse offset as binary: {}", e2.getMessage());
        }
      }
    } catch (Exception e) {
      log.debug("Error parsing package offset: {}", e.getMessage());
    }

    return null;
  }

  /**
   * Gets all BlockLocations from the global index (optimized method). BlockLocations contain
   * package_id, offset, and size information for direct block access. This is based on the original
   * TON C++ implementation where the global index maps: file_hash → (package_id, offset, size)
   *
   * @return Map of file hash to BlockLocation
   */
  public Map<String, BlockLocation> getAllBlockLocationsFromIndex() {
    Map<String, BlockLocation> blockLocations = new HashMap<>();

    log.info("Reading BlockLocations (package_id + offset + size) from global index...");

    if (globalIndexDbReader == null) {
      log.warn("Files database reader not available");
      return blockLocations;
    }

    // First, parse TL entries for debugging and understanding the structure
    parseTlEntries();

    AtomicInteger totalEntries = new AtomicInteger(0);
    AtomicInteger validLocations = new AtomicInteger(0);
    AtomicInteger parseErrors = new AtomicInteger(0);

    globalIndexDbReader
        .getGlobalIndexDb()
        .forEach(
            (key, value) -> {
              try {
                totalEntries.incrementAndGet();

                String hash = key.toString();
                if (!isValidHexString(hash)) {
                  return; // Skip non-hex keys
                }

                // Parse the value to extract package_id, offset, size
                byte[] valueBytes = value;
                BlockLocation location = parseGlobalIndexValue(hash, valueBytes);
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
        "Global index parsing: {} total entries, {} valid BlockLocations, {} parse errors",
        totalEntries.get(),
        validLocations.get(),
        parseErrors.get());

    return blockLocations;
  }

  /**
   * Parses a global index value to extract BlockLocation information. Based on the TL classes and
   * C++ implementation analysis, the Files database global index contains: 1. Main index entry: key
   * = db.files.index.key (empty), value = db.files.index.value (package lists) 2. Package metadata
   * entries: key = db.files.package.key, value = db.files.package.value 3. File hash entries: key =
   * file hash (raw bytes), value = location data (package_id, offset, size)
   *
   * @param hash The file hash (for reference)
   * @param value The global index value
   * @return BlockLocation or null if parsing fails
   */
  private BlockLocation parseGlobalIndexValue(String hash, byte[] value) {
    if (value == null || value.length < 4) {
      return null;
    }

    try {
      // For file hash entries (64-char hex strings), the value should be direct location data
      // This is NOT TL-serialized according to the original store_file/load_file pattern
      if (isValidHexString(hash) && hash.length() == 64) {
        return parseDirectLocationData(hash, value);
      }

      return null;

    } catch (Exception e) {
      log.debug("Error parsing global index value for hash {}: {}", hash, e.getMessage());
      return null;
    }
  }

  /**
   * Parses direct location data for file hash entries. Based on the original C++
   * serialize_location(package_id, offset, size) format. Format: [package_id: 8 bytes][offset: 8
   * bytes][size: 8 bytes] (little-endian)
   */
  private BlockLocation parseDirectLocationData(String hash, byte[] value) {
    if (value.length < 16) {
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
      log.debug("Error parsing direct location data for hash {}: {}", hash, e.getMessage());
      return null;
    }
  }

  /**
   * Parses TL-serialized entries in the global index. This handles the main index entry and package
   * metadata entries.
   */
  private void parseTlEntries() {
    if (globalIndexDbReader == null) {
      return;
    }

    try {
      // Try to read the main index entry using db.files.index.key (empty key)
      IndexKey indexKey = IndexKey.builder().build();
      byte[] indexKeyBytes = indexKey.serialize();

      byte[] indexValueBytes = globalIndexDbReader.getGlobalIndexDb().get(indexKeyBytes);
      if (indexValueBytes != null) {
        try {
          ByteBuffer buffer = ByteBuffer.wrap(indexValueBytes);
          IndexValue indexValue = IndexValue.deserialize(buffer);

          log.info(
              "Files database main index: {} packages, {} key packages, {} temp packages",
              indexValue.getPackages().size(),
              indexValue.getKeyPackages().size(),
              indexValue.getTempPackages().size());

          // Log some package IDs for debugging
          if (!indexValue.getPackages().isEmpty()) {
            log.info(
                "Sample package IDs: {}",
                indexValue.getPackages().subList(0, Math.min(5, indexValue.getPackages().size())));
          }

        } catch (Exception e) {
          log.debug("Error parsing main index value: {}", e.getMessage());
        }
      }

      // Try to read some package metadata entries
      // We can iterate through discovered package IDs and try to read their metadata
      // This is mainly for debugging and understanding the structure

    } catch (Exception e) {
      log.debug("Error parsing TL entries: {}", e.getMessage());
    }
  }

  /**
   * Attempts to parse TL-serialized location data. This handles cases where the value is a
   * TL-serialized object containing location info.
   */
  private BlockLocation parseTlSerializedLocation(String hash, byte[] value) {
    if (value.length < 8) {
      return null;
    }

    try {
      ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

      // Check for TL magic numbers that might indicate serialized data
      int magic = buffer.getInt();
      buffer.rewind();

      // For now, we don't have the exact TL schema, so we'll try to detect patterns
      // that look like serialized location data

      // If the first 4 bytes look like a reasonable magic number, skip them
      if (isLikelyTlMagic(magic)) {
        buffer.getInt(); // Skip magic

        if (buffer.remaining() >= 16) {
          long packageId = buffer.getLong();
          long offset = buffer.getLong();
          long size = 1024; // Default

          if (buffer.remaining() >= 8) {
            size = buffer.getLong();
          } else if (buffer.remaining() >= 4) {
            size = buffer.getInt() & 0xFFFFFFFFL;
          }

          if (packageId >= 0 && offset >= 0 && size > 0 && size < 100_000_000) {
            return BlockLocation.create(hash, packageId, offset, size);
          }
        }
      }

      return null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Attempts to parse direct binary location data. Format: [package_id: 8 bytes][offset: 8
   * bytes][size: 4-8 bytes] (little-endian)
   */
  private BlockLocation parseBinaryLocation(String hash, byte[] value) {
    if (value.length < 16) {
      return null;
    }

    try {
      ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

      // Extract package_id (8 bytes)
      long packageId = buffer.getLong();

      // Extract offset (8 bytes)
      long offset = buffer.getLong();

      // Extract size (variable length)
      long size = 1024; // Default size
      if (value.length >= 24) {
        size = buffer.getLong(); // 8-byte size
      } else if (value.length >= 20) {
        size = buffer.getInt() & 0xFFFFFFFFL; // 4-byte unsigned size
      }

      // Validate values - be more permissive for package IDs
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
      return null;
    }
  }

  /**
   * Attempts to parse string-based location data (like traditional archive indexes). Some entries
   * might just contain offset as string.
   */
  private BlockLocation parseStringLocation(String hash, byte[] value) {
    try {
      String valueStr = new String(value).trim();

      // Try to parse as simple offset string
      if (valueStr.matches("^\\d+$")) {
        long offset = Long.parseLong(valueStr);
        if (offset >= 0) {
          // For string-based offsets, we need to determine package ID from other means
          // This is a fallback - we'll use a synthetic package ID
          return BlockLocation.create(hash, 0, offset, 1024);
        }
      }

      return null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Checks if a 4-byte value looks like a TL magic number. TL magic numbers are typically in a
   * specific range.
   */
  private boolean isLikelyTlMagic(int magic) {
    // TL magic numbers are typically 32-bit values
    // They often have specific patterns, but without the exact schema,
    // we'll use heuristics based on common TL magic ranges

    // Convert to unsigned for comparison
    long unsignedMagic = magic & 0xFFFFFFFFL;

    // TL magics are often in certain ranges - this is a heuristic
    return (unsignedMagic > 0x10000000L && unsignedMagic < 0xF0000000L)
        || (magic == 0x7dc40502)
        || // db_filedb_key_empty (known from C++ code)
        (magic == 0xa504033e)
        || // db_filedb_key_blockFile
        (magic == 0x4ac6e727); // db_block_info
  }

  /**
   * Maps a package ID to its corresponding file path. This handles both Files database packages and
   * traditional archive packages.
   *
   * @param packageId The package identifier
   * @return The file path to the package, or null if not found
   */
  public String getPackagePathFromId(long packageId) {
    // Method 1: Check if it's a Files database package
    String filesPackagePath = getFilesPackagePath(packageId);
    if (filesPackagePath != null && Files.exists(Paths.get(filesPackagePath))) {
      return filesPackagePath;
    }

    // Method 2: Check traditional archive packages
    String archivePackagePath = getArchivePackagePath(packageId);
    if (archivePackagePath != null && Files.exists(Paths.get(archivePackagePath))) {
      return archivePackagePath;
    }

    // Method 3: Search through discovered archives
    for (Map.Entry<String, ArchiveInfo> entry : archiveInfos.entrySet()) {
      ArchiveInfo info = entry.getValue();
      if (info.id == packageId && info.packagePath != null) {
        return info.packagePath;
      }
    }

    log.debug("Package path not found for package ID: {}", packageId);
    return null;
  }

  /**
   * Gets the file path for a Files database package.
   *
   * @param packageId The package ID
   * @return The file path or null if not found
   */
  private String getFilesPackagePath(long packageId) {
    try {
      String packageFileName = String.format("%010d.pack", packageId);
      Path filesPackagesPath = Paths.get(rootPath, "..", "files", "packages");
      Path packagePath = filesPackagesPath.resolve(packageFileName);
      return packagePath.toString();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets the file path for a traditional archive package.
   *
   * @param packageId The package ID
   * @return The file path or null if not found
   */
  private String getArchivePackagePath(long packageId) {
    try {
      // Traditional archives are organized by ranges
      // arch0000 contains packages 0-99999, arch0001 contains 100000-199999, etc.
      int archiveDir = (int) (packageId / 100000);
      String archiveDirName = String.format("arch%04d", archiveDir);

      String packageFileName = String.format("archive.%05d.pack", packageId);
      Path archivePath = Paths.get(rootPath, "packages", archiveDirName, packageFileName);

      if (Files.exists(archivePath)) {
        return archivePath.toString();
      }

      // Also try key archives
      String keyArchiveDirName = String.format("key%03d", (int) (packageId / 1000000));
      String keyPackageFileName = String.format("key.archive.%06d.pack", packageId);
      Path keyArchivePath = Paths.get(rootPath, "packages", keyArchiveDirName, keyPackageFileName);

      if (Files.exists(keyArchivePath)) {
        return keyArchivePath.toString();
      }

      return null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets all BlockHandles from RocksDB index files (primary method). BlockHandles represent file
   * location information (offset and size) for files stored in packages.
   *
   * @return Map of file hash to BlockHandle
   */
  public Map<String, BlockHandle> getAllBlockHandlesFromIndex() {
    Map<String, BlockHandle> blockHandles = new HashMap<>();

    log.info("Reading BlockHandles (file location info) from RocksDB index files...");

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
          AtomicInteger fileHashKeys = new AtomicInteger(0);
          AtomicInteger newBlockHandles = new AtomicInteger(0);

          // Look for hex string keys (file hashes) that point to package offsets
          indexDb.forEach(
              (key, value) -> {
                try {
                  totalKeys.incrementAndGet();

                  // Debug: Analyze key types
                  String keyType = analyzeKeyType(key);
                  keyTypeStats.merge(keyType, 1, Integer::sum);

                  // Check if this is a file hash key (hex string)
                  String keyStr = new String(key);
                  if (isValidHexString(keyStr) && !keyStr.equals("status")) {
                    fileHashKeys.incrementAndGet();

                    // Parse the value as package offset
                    BlockHandle handle = parsePackageOffset(value);
                    if (handle != null) {
                      // Only add if we haven't seen this file hash before (deduplication)
                      if (!blockHandles.containsKey(keyStr)) {
                        blockHandles.put(keyStr, handle);
                        newBlockHandles.incrementAndGet();
                      }
                    }
                  }
                } catch (Exception e) {
                  log.debug("Error processing key in archive {}: {}", archiveKey, e.getMessage());
                }
              });

          log.info(
              "Archive {}: {} total keys, {} file hash keys, {} new BlockHandles, key types: {}",
              archiveKey,
              totalKeys.get(),
              fileHashKeys.get(),
              newBlockHandles.get(),
              keyTypeStats);
        } catch (IOException e) {
          log.warn("Error reading BlockHandles from archive {}: {}", archiveKey, e.getMessage());
        }
      }
    }

    log.info("Total unique BlockHandles found in indexes: {}", blockHandles.size());
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
          } catch (Throwable e) {
            log.debug(
                "Error parsing block for BlockHandle extraction from {}: {}", hash, e.getMessage());
            errorCount++;
            // Continue processing other entries instead of failing completely
          }
        }
      } catch (Throwable e) {
        log.debug("Error processing entry {}: {}", entry.getKey(), e.getMessage());
        errorCount++;
        // Continue processing other entries
      }
    }

    log.info(
        "Processed {} entries, {} errors, found {} BlockHandles from packages",
        processedCount,
        errorCount,
        blockHandles.size());
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
   */
  public Map<String, BlockHandle> getAllBlockHandles() {
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

          log.info(
              "Archive {}: Block={}, BlockProof={}, BlockHandle={}, Other={}",
              archiveKey,
              blockCount.get(),
              blockProofCount.get(),
              blockHandleCount.get(),
              otherCount.get());
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
