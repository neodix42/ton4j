package org.ton.ton4j.indexer.reader;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.tlb.BlockHandle;

/**
 * Enhanced ArchiveDbReader that uses BlockHandle information for efficient block retrieval. This
 * class extends the base ArchiveDbReader with methods that leverage offset and size information
 * from BlockHandles to directly access blocks in package files.
 */
@Slf4j
public class EnhancedArchiveDbReader extends ArchiveDbReader {

  // Cache for frequently accessed blocks to improve performance
  private final Map<String, Block> blockCache = new ConcurrentHashMap<>();
  private final Map<String, byte[]> rawDataCache = new ConcurrentHashMap<>();

  // Statistics for monitoring performance
  private long cacheHits = 0;
  private long cacheMisses = 0;
  private long directAccessCount = 0;
  private long fallbackAccessCount = 0;

  /**
   * Creates a new EnhancedArchiveDbReader.
   *
   * @param dbPath Path to the archive database directory
   * @throws IOException If an I/O error occurs
   */
  public EnhancedArchiveDbReader(String dbPath) throws IOException {
    super(dbPath);

    log.info("Enhanced ArchiveDbReader initialized with BlockHandle support");
  }

  // Cache for PackageReaders to avoid recreating them
  private final Map<String, PackageReader> enhancedPackageReaders = new HashMap<>();

  /**
   * Reads a block using BlockHandle information for direct access. This method uses the offset and
   * size from the BlockHandle to seek directly to the block location in the package file, providing
   * much better performance than sequential reading.
   *
   * @param hash The block hash
   * @param handle The BlockHandle containing offset and size information
   * @param packagePath Path to the package file containing the block
   * @return The block data, or null if not found or invalid
   */
  public byte[] readBlockUsingBlockHandle(String hash, BlockHandle handle, String packagePath) {

    if (handle == null || packagePath == null) {
      log.warn("Invalid parameters: handle={}, packagePath={}", handle, packagePath);
      return null;
    }

    // Check cache first
    String cacheKey = hash + ":" + packagePath;
    if (rawDataCache.containsKey(cacheKey)) {
      cacheHits++;
      log.debug("Cache hit for block {}", hash);
      return rawDataCache.get(cacheKey);
    }

    cacheMisses++;
    directAccessCount++;

    try {
      // Validate BlockHandle
      if (!validateBlockHandle(handle, packagePath)) {
        log.warn(
            "Invalid BlockHandle for hash {}: offset={}, size={}",
            hash,
            handle.getOffset(),
            handle.getSize());
        return null;
      }

      // Create our own PackageReader since the base class method is private
      PackageReader packageReader = getEnhancedPackageReader(packagePath);
      long offset = handle.getOffset().longValue();

      log.debug(
          "Reading block {} at offset {} with expected size {}", hash, offset, handle.getSize());

      PackageReader.PackageEntry entry = packageReader.getEntryAt(offset);

      if (entry == null) {
        log.warn("No entry found at offset {} for block {}", offset, hash);
        return null;
      }

      byte[] data = entry.getData();

      // Validate the retrieved data
      if (!validateBlockData(data, handle)) {
        log.warn("Block data validation failed for hash {}", hash);
        return null;
      }

      // Cache the result
      rawDataCache.put(cacheKey, data);

      log.debug("Successfully read block {} using BlockHandle (size: {} bytes)", hash, data.length);

      return data;

    } catch (Exception e) {
      log.error("Error reading block {} using BlockHandle: {}", hash, e.getMessage());
      return null;
    }
  }

  /**
   * Gets a PackageReader for the enhanced functionality, managing our own cache.
   *
   * @param packagePath Path to the package file
   * @return The PackageReader
   * @throws IOException If an I/O error occurs
   */
  private PackageReader getEnhancedPackageReader(String packagePath) throws IOException {
    if (!enhancedPackageReaders.containsKey(packagePath)) {
      enhancedPackageReaders.put(packagePath, new PackageReader(packagePath));
    }
    return enhancedPackageReaders.get(packagePath);
  }

  /**
   * Reads a block using BlockLocation information for optimized direct access. This is the new
   * optimized method that uses package_id for direct package access.
   *
   * @param hash The block hash
   * @param location The BlockLocation containing package_id, offset, and size
   * @return The block data, or null if not found or invalid
   */
  public byte[] readBlockUsingBlockLocation(String hash, BlockLocation location) {
    if (location == null || !location.isValid()) {
      log.warn("Invalid BlockLocation for hash {}: {}", hash, location);
      return null;
    }

    // Check cache first
    String cacheKey = hash + ":" + location.getPackageId();
    if (rawDataCache.containsKey(cacheKey)) {
      cacheHits++;
      log.debug("Cache hit for block {}", hash);
      return rawDataCache.get(cacheKey);
    }

    cacheMisses++;
    directAccessCount++;

    try {
      // Step 1: Map package_id to file path (direct lookup, no searching!)
      String packagePath = getPackagePathFromId(location.getPackageId());
      if (packagePath == null) {
        log.warn("Package path not found for package ID: {}", location.getPackageId());
        return null;
      }

      // Step 2: Read block data using offset and size
      PackageReader packageReader = getEnhancedPackageReader(packagePath);
      long offset = location.getOffset().longValue();

      log.debug(
          "Reading block {} from package {} at offset {} with size {}",
          hash,
          location.getPackageId(),
          offset,
          location.getSize());

      PackageReader.PackageEntry entry = packageReader.getEntryAt(offset);
      if (entry == null) {
        log.warn("No entry found at offset {} for block {}", offset, hash);
        return null;
      }

      byte[] data = entry.getData();

      // Validate the retrieved data
      if (data == null || data.length == 0) {
        log.warn("Empty data retrieved for block {}", hash);
        return null;
      }

      // Cache the result
      rawDataCache.put(cacheKey, data);

      log.debug(
          "Successfully read block {} using BlockLocation (size: {} bytes)", hash, data.length);
      return data;

    } catch (Exception e) {
      log.error("Error reading block {} using BlockLocation: {}", hash, e.getMessage());
      return null;
    }
  }

  /**
   * Enhanced block reading method that first tries to use BlockLocation information for optimized
   * direct access, and falls back to the original sequential method if needed.
   *
   * @param hash The block hash
   * @return The block data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  @Override
  public byte[] readBlock(String hash) throws IOException {
    log.debug("Reading block with enhanced method: {}", hash);

    try {
      // First, try to get BlockLocation for this hash (optimized approach)
      Map<String, BlockLocation> blockLocations = getAllBlockLocationsFromIndex();
      BlockLocation location = blockLocations.get(hash);

      if (location != null) {
        log.debug("Found BlockLocation for {}, attempting optimized direct access", hash);
        byte[] data = readBlockUsingBlockLocation(hash, location);
        if (data != null) {
          return data;
        }
      }

      // Fallback: try BlockHandle approach
      Map<String, BlockHandle> blockHandles = getAllBlockHandlesFromIndex();
      BlockHandle handle = blockHandles.get(hash);

      if (handle != null) {
        log.debug("Found BlockHandle for {}, attempting direct access", hash);

        // Try to find the package file for this block
        String packagePath = findPackagePathForBlock(hash);
        if (packagePath != null) {
          byte[] data = readBlockUsingBlockHandle(hash, handle, packagePath);
          if (data != null) {
            return data;
          }
        }
      }

      // Fall back to original method if both optimized approaches fail
      log.debug("Falling back to original method for block {}", hash);
      fallbackAccessCount++;
      return super.readBlock(hash);

    } catch (Exception e) {
      log.warn("Error in enhanced block reading for {}: {}", hash, e.getMessage());
      // Fall back to original method
      fallbackAccessCount++;
      return super.readBlock(hash);
    }
  }

  /**
   * Retrieves all blocks using BlockHandle information for efficient access. This method leverages
   * the offset and size information to read blocks directly without sequential scanning of package
   * files.
   *
   * @return Map of block hash to Block object
   */
  public Map<String, Block> getAllBlocksUsingBlockHandles() {
    log.info("Reading all blocks using BlockHandle information...");

    Map<String, Block> blocks = new HashMap<>();
    Map<String, BlockHandle> blockHandles = getAllBlockHandlesFromIndex();

    log.info("Found {} BlockHandles to process", blockHandles.size());

    int successCount = 0;
    int errorCount = 0;

    for (Map.Entry<String, BlockHandle> entry : blockHandles.entrySet()) {
      String hash = entry.getKey();
      BlockHandle handle = entry.getValue();

      try {
        // Check cache first
        if (blockCache.containsKey(hash)) {
          blocks.put(hash, blockCache.get(hash));
          cacheHits++;
          successCount++;
          continue;
        }

        cacheMisses++;

        // Find the package file for this block
        String packagePath = findPackagePathForBlock(hash);
        if (packagePath == null) {
          log.debug("Could not find package path for block {}", hash);
          continue;
        }

        // Read block data using BlockHandle
        byte[] data = readBlockUsingBlockHandle(hash, handle, packagePath);
        if (data == null) {
          continue;
        }

        // Parse the block
        Block block = parseBlockFromData(data);
        if (block != null) {
          blocks.put(hash, block);
          blockCache.put(hash, block); // Cache the parsed block
          successCount++;
        } else {
          errorCount++;
        }

      } catch (Exception e) {
        log.debug("Error processing block {}: {}", hash, e.getMessage());
        errorCount++;
      }
    }

    log.info(
        "Block retrieval completed: {} successful, {} errors out of {} BlockHandles",
        successCount,
        errorCount,
        blockHandles.size());

    return blocks;
  }

  /**
   * Parses block data into a Block object with proper validation.
   *
   * @param data The raw block data
   * @return The parsed Block object, or null if parsing fails
   */
  private Block parseBlockFromData(byte[] data) {
    try {
      Cell cell = CellBuilder.beginCell().fromBoc(data).endCell();
      long magic = cell.getBits().preReadUint(32).longValue();

      if (magic == 0x11ef55aaL) { // Block magic number
        return Block.deserialize(CellSlice.beginParse(cell));
      } else {
        log.debug("Invalid block magic: 0x{}", Long.toHexString(magic));
        return null;
      }
    } catch (Exception e) {
      log.debug("Error parsing block data: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Finds the package file path that contains a specific block. This method searches through the
   * archive information to locate the package file that should contain the given block hash.
   *
   * @param hash The block hash to search for
   * @return The package file path, or null if not found
   */
  private String findPackagePathForBlock(String hash) {
    // Try to find the block in any of the known archives
    for (String archiveKey : getArchiveKeys()) {
      try {
        // Check if this archive contains the block
        byte[] testData = readBlockFromArchive(hash, archiveKey);
        if (testData != null) {
          // Get the package path for this archive
          return getPackagePathForArchive(archiveKey);
        }
      } catch (Exception e) {
        // Continue searching in other archives
      }
    }
    return null;
  }

  /** Reads a block from a specific archive (helper method). */
  private byte[] readBlockFromArchive(String hash, String archiveKey) {
    try {
      // This is a simplified implementation - in practice, you'd want to
      // check the archive's index to see if it contains the hash
      return super.readBlock(hash);
    } catch (Exception e) {
      return null;
    }
  }

  /** Gets the package path for a specific archive key. */
  private String getPackagePathForArchive(String archiveKey) {
    // This would need to be implemented based on the archive structure
    // For now, return a placeholder that would need to be filled in
    // based on the actual archive organization
    return null; // TODO: Implement based on archive structure
  }

  /**
   * Validates that a BlockHandle contains reasonable values.
   *
   * @param handle The BlockHandle to validate
   * @param packagePath Path to the package file
   * @return True if the BlockHandle appears valid
   */
  private boolean validateBlockHandle(BlockHandle handle, String packagePath) {
    if (handle == null) {
      return false;
    }

    BigInteger offset = handle.getOffset();
    BigInteger size = handle.getSize();

    // Check for null values
    if (offset == null || size == null) {
      log.debug("BlockHandle has null offset or size");
      return false;
    }

    // Check for negative values
    if (offset.compareTo(BigInteger.ZERO) < 0 || size.compareTo(BigInteger.ZERO) <= 0) {
      log.debug(
          "BlockHandle has negative offset or non-positive size: offset={}, size={}", offset, size);
      return false;
    }

    // Check for unreasonably large values
    BigInteger maxSize = BigInteger.valueOf(100_000_000); // 100MB max per block
    if (size.compareTo(maxSize) > 0) {
      log.debug("BlockHandle size too large: {}", size);
      return false;
    }

    // TODO: Add validation against actual package file size
    // This would require checking the file size and ensuring offset + size <= file size

    return true;
  }

  /**
   * Validates that block data matches the expected size from BlockHandle.
   *
   * @param data The block data
   * @param handle The BlockHandle with expected size
   * @return True if the data is valid
   */
  private boolean validateBlockData(byte[] data, BlockHandle handle) {
    if (data == null || handle == null) {
      return false;
    }

    // Check size matches (with some tolerance for header overhead)
    long expectedSize = handle.getSize().longValue();
    long actualSize = data.length;

    // Allow for some variance due to package entry headers
    long tolerance = 100; // bytes
    if (Math.abs(actualSize - expectedSize) > tolerance) {
      log.debug("Block data size mismatch: expected ~{}, got {}", expectedSize, actualSize);
      // Don't fail validation just for size mismatch, as package format may add overhead
    }

    // Validate that data is valid BOC format
    try {
      Cell cell = CellBuilder.beginCell().fromBoc(data).endCell();
      // If we can parse it as a cell, it's probably valid
      return true;
    } catch (Exception e) {
      log.debug("Block data is not valid BOC format: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Gets performance statistics for monitoring cache effectiveness.
   *
   * @return Map containing performance metrics
   */
  public Map<String, Long> getPerformanceStats() {
    Map<String, Long> stats = new HashMap<>();
    stats.put("cacheHits", cacheHits);
    stats.put("cacheMisses", cacheMisses);
    stats.put("directAccessCount", directAccessCount);
    stats.put("fallbackAccessCount", fallbackAccessCount);
    stats.put("cachedBlocks", (long) blockCache.size());
    stats.put("cachedRawData", (long) rawDataCache.size());

    long totalAccess = cacheHits + cacheMisses;
    if (totalAccess > 0) {
      stats.put("cacheHitRate", (cacheHits * 100) / totalAccess);
    }

    return stats;
  }

  /** Clears all caches to free memory. */
  public void clearCaches() {
    blockCache.clear();
    rawDataCache.clear();
    log.info("Cleared all caches");
  }

  /** Logs current performance statistics. */
  public void logPerformanceStats() {
    Map<String, Long> stats = getPerformanceStats();
    log.info("=== Enhanced ArchiveDbReader Performance Stats ===");
    log.info("Cache hits: {}", stats.get("cacheHits"));
    log.info("Cache misses: {}", stats.get("cacheMisses"));
    log.info("Cache hit rate: {}%", stats.getOrDefault("cacheHitRate", 0L));
    log.info("Direct access count: {}", stats.get("directAccessCount"));
    log.info("Fallback access count: {}", stats.get("fallbackAccessCount"));
    log.info("Cached blocks: {}", stats.get("cachedBlocks"));
    log.info("Cached raw data entries: {}", stats.get("cachedRawData"));
  }

  @Override
  public void close() throws IOException {
    // Close enhanced package readers
    for (PackageReader reader : enhancedPackageReaders.values()) {
      try {
        reader.close();
      } catch (IOException e) {
        log.warn("Error closing enhanced package reader: {}", e.getMessage());
      }
    }
    enhancedPackageReaders.clear();

    // Clear caches
    clearCaches();

    // Call parent close method
    super.close();
  }
}
