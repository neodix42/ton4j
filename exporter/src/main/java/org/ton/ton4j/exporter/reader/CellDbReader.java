package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.exporter.types.*;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.types.db.celldb.Value;
import org.ton.ton4j.utils.Utils;

/**
 * Reader for TON CellDB database. The CellDB stores blockchain state cells in a RocksDB database
 * with a linked-list structure for metadata entries.
 *
 * <p>Based on the original TON C++ implementation in celldb.cpp, the CellDB uses: 1. Metadata
 * entries: key = "desc" + SHA256(TL-serialized block_id), value = TL-serialized db.celldb.value 2.
 * Special empty entry: key = "desczero", value = TL-serialized db.celldb.value (sentinel for linked
 * list) 3. Cell data entries: key = cell hash (raw bytes), value = serialized cell content 4.
 * Linked list structure: entries connected via prev/next pointers forming a doubly-linked list
 *
 * <p>The TL types used: - db.celldb.value: block_id:tonNode.blockIdExt prev:int256 next:int256
 * root_hash:int256 - db.celldb.key.value: hash:int256
 */
@Slf4j
@Data
public class CellDbReader implements Closeable {

  private final String dbPath;
  private RocksDbWrapper cellDb;

  // Cache for parsed entries
  private final Map<String, Value> entryCache = new HashMap<>();
  private Value emptyEntry;

  /**
   * Creates a new CellDbReader.
   *
   * @param dbPath Path to the database root directory (should contain celldb subdirectory)
   * @throws IOException If an I/O error occurs
   */
  public CellDbReader(String dbPath) throws IOException {
    this.dbPath = dbPath;
    initializeCellDatabase();
    loadEmptyEntry();
  }

  /** Initializes the CellDB database connection. */
  private void initializeCellDatabase() throws IOException {
    Path cellDbPath = Paths.get(dbPath, "celldb");

    if (!Files.exists(cellDbPath)) {
      throw new IOException("CellDB database not found at: " + cellDbPath);
    }

    try {
      cellDb = new RocksDbWrapper(cellDbPath.toString());
      log.info("Initialized CellDB database: {}", cellDbPath);
    } catch (IOException e) {
      throw new IOException("Could not initialize CellDB database: " + e.getMessage(), e);
    }
  }

  /** Loads the empty/sentinel entry that serves as the head of the linked list. */
  private void loadEmptyEntry() throws IOException {
    try {
      String emptyKey = getEmptyKey();
      byte[] emptyValueBytes = cellDb.get(emptyKey.getBytes());

      if (emptyValueBytes != null) {
        ByteBuffer buffer = ByteBuffer.wrap(emptyValueBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Skip the TL constructor ID (4 bytes)
        if (buffer.remaining() >= 4) {
          int constructor = buffer.getInt();
          log.debug("Empty entry TL constructor ID: 0x{}", Integer.toHexString(constructor));
        }

        emptyEntry = Value.deserialize(buffer);
        log.debug(
            "Loaded empty entry: prev={}, next={}", emptyEntry.getPrev(), emptyEntry.getNext());
      } else {
        log.warn("Empty entry not found in CellDB");
        // Create a placeholder empty entry
        emptyEntry =
            Value.builder()
                .blockId(getEmptyBlockId())
                .prev(new byte[32])
                .next(new byte[32])
                .rootHash(new byte[32])
                .build();
      }
    } catch (Exception e) {
      throw new IOException("Error loading empty entry: " + e.getMessage(), e);
    }
  }

  /**
   * Gets all cell entries from the CellDB as a map of key hash to Value. This method scans all
   * metadata entries (keys starting with "desc").
   *
   * @return Map of key hash to CellDB Value
   */
  public Map<String, Value> getAllCellEntries() {
    Map<String, Value> cellEntries = new HashMap<>();

    log.info("Reading all cell entries from CellDB...");

    AtomicInteger totalEntries = new AtomicInteger(0);
    AtomicInteger validEntries = new AtomicInteger(0);
    AtomicInteger parseErrors = new AtomicInteger(0);

    cellDb.forEach(
        (key, value) -> {
          try {
            totalEntries.incrementAndGet();
            String keyStr = new String(key);

            // Skip non-metadata keys
            if (!keyStr.startsWith("desc")) {
              return;
            }

            // Parse the TL-serialized value
            // TL format includes a 4-byte constructor ID at the beginning
            ByteBuffer buffer = ByteBuffer.wrap(value);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // Skip the TL constructor ID (4 bytes)
            if (buffer.remaining() >= 4) {
              int constructor = buffer.getInt();
              // Log the constructor for debugging
              //              if (validEntries.get() < 5) {
              //                log.debug("TL constructor ID: 0x{}",
              // Integer.toHexString(constructor));
              //              }
            }

            Value cellValue = Value.deserialize(buffer);

            // Extract key hash from the key (remove "desc" prefix for regular entries)
            String keyHash;
            if ("desczero".equals(keyStr)) {
              keyHash = ""; // Empty key hash for sentinel entry
            } else {
              // The key is "desc" + Base64 hash, so extract the Base64 part
              // and convert it to hex for consistent storage
              String base64Part = keyStr.substring(4); // Remove "desc" prefix
              try {
                // Decode Base64 to get the raw hash bytes, then convert to hex
                byte[] hashBytes = java.util.Base64.getDecoder().decode(base64Part);
                keyHash = Utils.bytesToHex(hashBytes);
              } catch (Exception e) {
                log.debug("Error decoding Base64 key hash {}: {}", base64Part, e.getMessage());
                keyHash = base64Part; // Fallback to Base64 part
              }
            }

            cellEntries.put(keyHash, cellValue);
            entryCache.put(keyHash, cellValue);
            validEntries.incrementAndGet();

            //        if (validEntries.get() % 1000 == 0) {
            //          log.debug("Processed {} cell entries", validEntries.get());
            //        }

          } catch (Exception e) {
            parseErrors.incrementAndGet();
            log.debug("Error processing CellDB entry: {}", e.getMessage());
          }
        });

    log.info(
        "CellDB parsing: {} total entries, {} valid cell entries, {} parse errors",
        totalEntries.get(),
        validEntries.get(),
        parseErrors.get());

    return cellEntries;
  }

  /**
   * Gets a specific cell entry by block ID.
   *
   * @param blockId The block ID to look up
   * @return The CellDB Value, or null if not found
   */
  public Value getCellEntry(BlockIdExt blockId) {
    try {
      String keyHash = getKeyHash(blockId);
      return getCellEntryByHash(keyHash);
    } catch (Exception e) {
      log.debug("Error getting cell entry for block {}: {}", blockId, e.getMessage());
      return null;
    }
  }

  /**
   * Gets a specific cell entry by key hash.
   *
   * @param keyHash The key hash to look up
   * @return The CellDB Value, or null if not found
   */
  public Value getCellEntryByHash(String keyHash) {
    // Check cache first
    if (entryCache.containsKey(keyHash)) {
      return entryCache.get(keyHash);
    }

    try {
      String key = getKey(keyHash);
      byte[] valueBytes = cellDb.get(key.getBytes());

      if (valueBytes == null) {
        return null;
      }

      ByteBuffer buffer = ByteBuffer.wrap(valueBytes);
      buffer.order(ByteOrder.LITTLE_ENDIAN);

      // Skip the TL constructor ID (4 bytes)
      if (buffer.remaining() >= 4) {
        buffer.getInt();
      }

      Value cellValue = Value.deserialize(buffer);

      // Cache the result
      entryCache.put(keyHash, cellValue);
      return cellValue;

    } catch (Exception e) {
      log.debug("Error getting cell entry by hash {}: {}", keyHash, e.getMessage());
      return null;
    }
  }

  /**
   * Determines the cell type from cell data by analyzing the cell header. Based on TON cell format
   * specification.
   *
   * @param cellData The raw cell data
   * @return The determined cell type
   */
  public CellType determineCellType(byte[] cellData) {
    if (cellData == null || cellData.length == 0) {
      return CellType.ORDINARY;
    }

    try {
      // Parse cell header to determine special type
      // Cell format: [descriptor1][descriptor2][data][refs]
      // descriptor1 contains: refs_count(3) + exotic(1) + has_hashes(1) + level(3)
      // descriptor2 contains: data_bits(7) + completion_tag(1)

      if (cellData.length < 2) {
        return CellType.ORDINARY;
      }

      byte descriptor1 = cellData[0];

      // Extract exotic flag (bit 3 from right, 0-indexed)
      boolean isExotic = (descriptor1 & 0x08) != 0;

      if (!isExotic) {
        return CellType.ORDINARY;
      }

      // For exotic cells, the type is determined by the first few bits of data
      if (cellData.length < 3) {
        return CellType.ORDINARY;
      }

      // Skip descriptors and look at first data byte
      byte descriptor2 = cellData[1];
      int dataBits = (descriptor2 & 0xFE) >> 1; // Extract data_bits (7 bits)

      if (dataBits >= 8 && cellData.length > 2) {
        byte firstDataByte = cellData[2];

        // Extract special type from first 3 bits of data
        int specialType = (firstDataByte >> 5) & 0x07;
        return CellType.fromTypeId(specialType);
      }

      return CellType.ORDINARY;

    } catch (Exception e) {
      log.debug("Error determining cell type: {}", e.getMessage());
      return CellType.ORDINARY;
    }
  }

  /**
   * Extracts cell references from cell data. This is a simplified implementation that extracts
   * reference hashes.
   *
   * @param cellData The raw cell data
   * @return List of referenced cell hashes
   */
  public List<String> extractCellReferences(byte[] cellData) {
    List<String> references = new ArrayList<>();

    if (cellData == null || cellData.length < 2) {
      return references;
    }

    try {
      byte descriptor1 = cellData[0];
      byte descriptor2 = cellData[1];

      // Extract reference count from descriptor1 (first 3 bits)
      int refCount = descriptor1 & 0x07;

      // Extract data bits from descriptor2
      int dataBits = (descriptor2 & 0xFE) >> 1;
      int dataBytes = (dataBits + 7) / 8; // Round up to nearest byte

      // Calculate where references start
      int refsStartOffset = 2 + dataBytes; // 2 descriptors + data bytes

      // Each reference is 32 bytes (256 bits)
      for (int i = 0; i < refCount && refsStartOffset + (i + 1) * 32 <= cellData.length; i++) {
        int refOffset = refsStartOffset + i * 32;
        byte[] refHash = Arrays.copyOfRange(cellData, refOffset, refOffset + 32);
        references.add(Utils.bytesToHex(refHash));
      }

    } catch (Exception e) {
      log.debug("Error extracting cell references: {}", e.getMessage());
    }

    return references;
  }

  /**
   * Traverses the cell tree starting from a root hash and collects all child cells. This implements
   * breadth-first traversal to avoid deep recursion.
   *
   * @param rootHash The root cell hash to start traversal from
   * @param maxDepth Maximum depth to traverse (0 for unlimited)
   * @return Set of all cell hashes in the tree
   */
  public Set<String> getAllChildCells(String rootHash, int maxDepth) {
    Set<String> allCells = new HashSet<>();
    Set<String> visited = new HashSet<>();
    Queue<CellTraversalNode> toProcess = new LinkedList<>();

    toProcess.add(new CellTraversalNode(rootHash, 0));

    while (!toProcess.isEmpty()) {
      CellTraversalNode current = toProcess.poll();

      if (visited.contains(current.hash)) {
        continue;
      }

      if (maxDepth > 0 && current.depth >= maxDepth) {
        continue;
      }

      visited.add(current.hash);
      allCells.add(current.hash);

      try {
        byte[] cellData = readCellData(current.hash);
        if (cellData != null) {
          List<String> childRefs = extractCellReferences(cellData);
          for (String childRef : childRefs) {
            if (!visited.contains(childRef)) {
              toProcess.add(new CellTraversalNode(childRef, current.depth + 1));
            }
          }
        }
      } catch (IOException e) {
        log.debug("Error reading cell data for {}: {}", current.hash, e.getMessage());
      }

      // Safety check to prevent infinite loops
      if (allCells.size() > 1000000) {
        log.warn("Cell tree traversal stopped: too many cells (possible loop or very large tree)");
        break;
      }
    }

    log.info("Cell tree traversal from {} found {} cells", rootHash, allCells.size());
    return allCells;
  }

  /** Convenience method for unlimited depth traversal. */
  public Set<String> getAllChildCells(String rootHash) {
    return getAllChildCells(rootHash, 0);
  }

  /**
   * Analyzes cell tree statistics for a given root hash.
   *
   * @param rootHash The root cell hash
   * @return Statistics about the cell tree
   */
  public CellTreeStatistics analyzeCellTree(String rootHash) {
    Set<String> allCells = getAllChildCells(rootHash, 10); // Limit depth for performance

    Map<CellType, Integer> typeCounts = new HashMap<>();
    int totalSize = 0;
    int maxDepth = 0;

    for (String cellHash : allCells) {
      try {
        byte[] cellData = readCellData(cellHash);
        if (cellData != null) {
          CellType type = determineCellType(cellData);
          typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
          totalSize += cellData.length;
        }
      } catch (IOException e) {
        log.debug("Error analyzing cell {}: {}", cellHash, e.getMessage());
      }
    }

    return new CellTreeStatistics(rootHash, allCells.size(), typeCounts, totalSize, maxDepth);
  }

  /**
   * Gets the empty/sentinel entry.
   *
   * @return The empty entry
   */
  public Value getEmptyEntry() {
    return emptyEntry;
  }

  /**
   * Gets all cell hashes that have binary data stored in the database. This scans for raw hash keys
   * (32 bytes) that are not metadata entries.
   *
   * @return Set of cell hashes (hex strings)
   */
  public Set<String> getAllCellHashes() {
    Set<String> cellHashes = new HashSet<>();

    cellDb.forEach(
        (key, value) -> {
          try {
            // Look for raw hash keys (32 bytes, not starting with "desc")
            if (key.length == 32) {
              String hash = Utils.bytesToHex(key);
              cellHashes.add(hash);
            }
          } catch (Exception e) {
            log.debug("Error processing potential cell hash key: {}", e.getMessage());
          }
        });

    log.info("Found {} cell hashes with binary data", cellHashes.size());
    return cellHashes;
  }

  /**
   * Reads binary cell data by root hash.
   *
   * @param rootHash The root hash (hex string)
   * @return The cell data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public byte[] readCellData(String rootHash) throws IOException {
    try {
      byte[] hashBytes = Utils.hexToSignedBytes(rootHash);
      return cellDb.get(hashBytes);
    } catch (Exception e) {
      log.debug("Error reading cell data for hash {}: {}", rootHash, e.getMessage());
      return null;
    }
  }

  /**
   * Checks if binary data exists for a hash.
   *
   * @param hash The hash (hex string)
   * @return True if binary data exists, false otherwise
   */
  public boolean hasCellData(String hash) {
    try {
      byte[] hashBytes = Utils.hexToSignedBytes(hash);
      byte[] data = cellDb.get(hashBytes);
      return data != null;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Gets hash-to-offset mappings for consistency with other readers. Note: CellDB doesn't use
   * offset-based storage like archive packages, so this returns hash-to-size mappings instead.
   *
   * @return Map of hash to data size
   */
  public Map<String, Long> getAllHashSizeMappings() {
    Map<String, Long> hashSizeMappings = new HashMap<>();

    cellDb.forEach(
        (key, value) -> {
          try {
            // Look for raw hash keys (32 bytes, not starting with "desc")
            if (key.length == 32) {
              String hash = Utils.bytesToHex(key);
              hashSizeMappings.put(hash, (long) value.length);
            }
          } catch (Exception e) {
            log.debug("Error processing hash-size mapping: {}", e.getMessage());
          }
        });

    log.info("Generated {} hash-to-size mappings", hashSizeMappings.size());
    return hashSizeMappings;
  }

  /**
   * Generates the database key for a given key hash. Following C++ implementation: "desc" + keyHash
   * for regular entries.
   *
   * @param keyHash The key hash (hex string)
   * @return The database key
   */
  private static String getKey(String keyHash) {
    if (keyHash == null || keyHash.isEmpty()) {
      return getEmptyKey();
    }
    return "desc" + keyHash;
  }

  /**
   * Generates the key hash for a block ID by SHA256 hashing the TL-serialized block ID. This
   * follows the C++ implementation logic.
   *
   * @param blockId The block ID
   * @return The key hash (hex string)
   */
  private static String getKeyHash(BlockIdExt blockId) {
    if (blockId == null || !isValidBlockId(blockId)) {
      return "";
    }

    try {
      // Serialize the block ID to TL format
      byte[] serializedBlockId = blockId.serialize();

      // Calculate SHA256 hash
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(serializedBlockId);

      return Utils.bytesToHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  /**
   * Gets the key for the empty/sentinel entry.
   *
   * @return The empty entry key
   */
  private static String getEmptyKey() {
    return "desczero";
  }

  /**
   * Creates an empty block ID for the sentinel entry. Following C++ implementation:
   * workchainInvalid = 0x80000000 (-2147483648)
   *
   * @return Empty block ID
   */
  private static BlockIdExt getEmptyBlockId() {
    return BlockIdExt.builder()
        .workchain(0x80000000) // workchainInvalid from TON C++ code
        .shard(0)
        .seqno(0)
        .rootHash(new byte[32])
        .fileHash(new byte[32])
        .build();
  }

  /**
   * Checks if a block ID represents the empty/sentinel entry. Following C++ implementation:
   * workchainInvalid = 0x80000000, masterchainId = -1
   *
   * @param blockId The block ID to check
   * @return True if this is an empty block ID
   */
  private static boolean isEmptyBlockId(BlockIdExt blockId) {
    if (blockId == null) {
      return true;
    }

    // Check for invalid workchain (sentinel marker)
    // Note: workchain -1 is valid (masterchain), 0x80000000 is invalid
    return blockId.getWorkchain() == 0x80000000;
  }

  /**
   * Checks if a block ID is valid (not null and has reasonable values). Following C++
   * implementation: workchainInvalid = 0x80000000, masterchainId = -1
   *
   * @param blockId The block ID to check
   * @return True if valid
   */
  private static boolean isValidBlockId(BlockIdExt blockId) {
    return blockId != null
        && blockId.getWorkchain() != 0x80000000
        && blockId.getRootHash() != null
        && blockId.getFileHash() != null;
  }

  /**
   * Gets cell data for a specific metadata entry using its root hash. This demonstrates the
   * connection between metadata entries and actual cell data.
   *
   * @param entry The metadata entry
   * @return The cell data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public byte[] getCellDataForEntry(Value entry) throws IOException {
    if (entry == null || entry.getRootHash() == null) {
      return null;
    }
    return readCellData(entry.getRootHash());
  }

  /**
   * Analyzes the relationship between metadata entries and cell data. This method provides insights
   * into the dual storage structure of CellDB.
   *
   * @return Analysis results showing connections between metadata and cell data
   */
  public CellDataAnalysis analyzeCellDataRelationships() {
    log.info("Analyzing CellDB metadata -> cell data relationships...");

    Map<String, Value> metadata = getAllCellEntries();
    Set<String> cellHashes = getAllCellHashes();

    int connectedEntries = 0;
    int orphanedCellData = 0;
    int missingCellData = 0;

    Set<String> referencedHashes = new HashSet<>();

    // Check metadata -> cell data connections
    for (Value entry : metadata.values()) {
      if (entry.getRootHash() != null) {
        String rootHash = entry.getRootHash();
        referencedHashes.add(rootHash);

        if (cellHashes.contains(rootHash)) {
          connectedEntries++;
        } else {
          missingCellData++;
          log.debug("Missing cell data for root hash: {}", rootHash);
        }
      }
    }

    // Check for orphaned cell data
    for (String hash : cellHashes) {
      if (!referencedHashes.contains(hash)) {
        orphanedCellData++;
      }
    }

    CellDataAnalysis analysis =
        new CellDataAnalysis(
            metadata.size(),
            cellHashes.size(),
            connectedEntries,
            missingCellData,
            orphanedCellData);

    log.info("CellDB analysis completed: {}", analysis);
    return analysis;
  }

  /**
   * Gets detailed information about metadata entries and their corresponding cell data. This method
   * demonstrates the relationship between the two storage types.
   *
   * @param maxEntries Maximum number of entries to analyze (0 for all)
   * @return Map of metadata key to cell data info
   */
  public Map<String, CellDataInfo> getDetailedCellDataInfo(int maxEntries) {
    Map<String, CellDataInfo> detailedInfo = new HashMap<>();
    Map<String, Value> metadata = getAllCellEntries();

    int count = 0;
    for (Map.Entry<String, Value> entry : metadata.entrySet()) {
      if (maxEntries > 0 && count >= maxEntries) {
        break;
      }

      String keyHash = entry.getKey();
      Value value = entry.getValue();
      String rootHash = value.getRootHash();

      try {
        byte[] cellData = readCellData(rootHash);
        boolean hasData = cellData != null;
        int dataSize = hasData ? cellData.length : 0;

        CellDataInfo info =
            new CellDataInfo(keyHash, rootHash, value.getBlockId(), hasData, dataSize);

        detailedInfo.put(keyHash, info);
        count++;

      } catch (IOException e) {
        log.debug("Error reading cell data for {}: {}", rootHash, e.getMessage());
        CellDataInfo info = new CellDataInfo(keyHash, rootHash, value.getBlockId(), false, 0);
        detailedInfo.put(keyHash, info);
      }
    }

    log.info("Generated detailed cell data info for {} entries", detailedInfo.size());
    return detailedInfo;
  }

  /**
   * Gets statistics about the CellDB.
   *
   * @return Map of statistic name to value
   */
  public Map<String, Object> getStatistics() {
    Map<String, Object> stats = new HashMap<>();

    try {
      Map<String, Value> allEntries = getAllCellEntries();
      Set<String> cellHashes = getAllCellHashes();

      stats.put("total_metadata_entries", allEntries.size());
      stats.put("total_cell_data_entries", cellHashes.size());
      stats.put("empty_entry_available", emptyEntry != null);

      if (emptyEntry != null) {
        stats.put("empty_entry_prev", emptyEntry.getPrev());
        stats.put("empty_entry_next", emptyEntry.getNext());
      }

      // Count entries by workchain
      Map<Integer, Integer> workchainCounts = new HashMap<>();
      for (Value entry : allEntries.values()) {
        if (entry.getBlockId() != null) {
          int workchain = entry.getBlockId().getWorkchain();
          workchainCounts.put(workchain, workchainCounts.getOrDefault(workchain, 0) + 1);
        }
      }
      stats.put("entries_by_workchain", workchainCounts);

      // Add relationship analysis
      CellDataAnalysis analysis = analyzeCellDataRelationships();
      stats.put("connection_percentage", analysis.getConnectionPercentage());
      stats.put("orphaned_percentage", analysis.getOrphanedPercentage());
      stats.put("database_healthy", analysis.isHealthy());

    } catch (Exception e) {
      log.error("Error generating statistics: {}", e.getMessage());
      stats.put("error", e.getMessage());
    }

    return stats;
  }

  @Override
  public void close() throws IOException {
    if (cellDb != null) {
      cellDb.close();
      log.debug("Closed CellDB database");
    }
  }
}
