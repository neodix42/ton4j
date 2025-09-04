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
import org.ton.ton4j.address.Address;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.LevelMask;
import org.ton.ton4j.exporter.types.*;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.types.db.celldb.Value;
import org.ton.ton4j.tlb.Account;
import org.ton.ton4j.tlb.ShardAccount;
import org.ton.ton4j.tlb.ShardAccounts;
import org.ton.ton4j.tlb.ShardStateUnsplit;
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
              //              int constructor =
              buffer.getInt();
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
   * Gets a cell entry by its key hash. This follows the C++ implementation pattern.
   *
   * @param keyHash The key hash (hex string)
   * @return The cell entry, or null if not found
   */
  public Value getCellEntryByHash(String keyHash) {
    if (entryCache.containsKey(keyHash)) {
      return entryCache.get(keyHash);
    }

    try {
      String key = getKey(keyHash);
      byte[] valueBytes = cellDb.get(key.getBytes());

      if (valueBytes != null) {
        ByteBuffer buffer = ByteBuffer.wrap(valueBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Skip the TL constructor ID (4 bytes)
        if (buffer.remaining() >= 4) {
          buffer.getInt();
        }

        Value cellValue = Value.deserialize(buffer);
        entryCache.put(keyHash, cellValue);
        return cellValue;
      }
    } catch (Exception e) {
      log.debug("Error getting cell entry by hash {}: {}", keyHash, e.getMessage());
    }

    return null;
  }

  /**
   * Gets a cell entry by BlockIdExt. This follows the C++ implementation pattern.
   *
   * @param blockId The block ID
   * @return The cell entry, or null if not found
   */
  public Value getCellEntry(BlockIdExt blockId) {
    if (blockId == null) {
      return null;
    }

    try {
      String keyHash = getKeyHash(blockId);
      return getCellEntryByHash(keyHash);
    } catch (Exception e) {
      log.debug("Error getting cell entry by BlockId {}: {}", blockId, e.getMessage());
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
    // Convert hex string to Base64 for the key, as that's how it's stored in the database
    try {
      byte[] hashBytes = Utils.hexToSignedBytes(keyHash);
      String base64Hash = java.util.Base64.getEncoder().encodeToString(hashBytes);
      return "desc" + base64Hash;
    } catch (Exception e) {
      // Fallback to direct concatenation if hex conversion fails
      return "desc" + keyHash;
    }
  }

  /**
   * Generates the key hash for a block ID by SHA256 hashing the TL-serialized block ID. This
   * follows the C++ implementation logic.
   *
   * @param blockId The block ID
   * @return The key hash (hex string)
   */
  private static String getKeyHash(BlockIdExt blockId) {
    if (!isValidBlockId(blockId)) {
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

  /**
   * Retrieves and deserializes an Account TL-B object by cell hash. This method converts raw cell
   * data to an Account object using the TON TL-B deserialization process.
   *
   * @param cellHash The cell hash (hex string) containing Account data
   * @return The deserialized Account object, or null if not found or not a valid Account cell
   * @throws IOException If an I/O error occurs during cell data retrieval
   */
  public Account retrieveAccountByHash(String cellHash) throws IOException {
    if (cellHash == null || cellHash.isEmpty()) {
      log.debug("Invalid cell hash provided: {}", cellHash);
      return null;
    }

    try {
      // 1. Retrieve raw cell data
      byte[] cellData = readCellData(cellHash);
      if (cellData == null) {
        log.debug("No cell data found for hash: {}", cellHash);
        return null;
      }

      // 2. Parse cell from CellDB storage format
      Cell cell = parseCellFromCellDbFormat(cellData);
      if (cell == null) {
        log.debug("Failed to parse cell from CellDB format for hash: {}", cellHash);
        return null;
      }

      // 3. Create CellSlice for TL-B deserialization
      CellSlice cellSlice = CellSlice.beginParse(cell);

      // 4. Deserialize to Account using TL-B deserializer
      Account account = Account.deserialize(cellSlice);

      log.debug(
          "Successfully retrieved Account for hash: {} (isNone: {})", cellHash, account.isNone());
      return account;

    } catch (Exception e) {
      log.debug("Failed to retrieve Account for hash {}: {}", cellHash, e.getMessage());
      return null; // Graceful handling - not all cells are Accounts
    }
  }

  /**
   * Retrieves and deserializes multiple Account TL-B objects by their cell hashes. This method
   * processes accounts in batch for better performance.
   *
   * @param cellHashes Set of cell hashes to retrieve Account objects for
   * @return Map of cell hash to Account object (only successful retrievals included)
   */
  public Map<String, Account> retrieveAccountsByHashes(Set<String> cellHashes) {
    Map<String, Account> accounts = new HashMap<>();

    if (cellHashes == null || cellHashes.isEmpty()) {
      log.debug("No cell hashes provided for batch Account retrieval");
      return accounts;
    }

    log.info("Starting batch Account retrieval for {} hashes", cellHashes.size());

    int processed = 0;
    int successful = 0;
    int failed = 0;

    for (String cellHash : cellHashes) {
      try {
        Account account = retrieveAccountByHash(cellHash);
        if (account != null) {
          accounts.put(cellHash, account);
          successful++;
        } else {
          failed++;
        }
      } catch (IOException e) {
        log.debug("I/O error retrieving Account for hash {}: {}", cellHash, e.getMessage());
        failed++;
      }

      processed++;
      if (processed % 1000 == 0) {
        log.info(
            "Batch Account retrieval progress: {}/{} processed, {} successful, {} failed",
            processed,
            cellHashes.size(),
            successful,
            failed);
      }
    }

    log.info(
        "Batch Account retrieval completed: {}/{} successful, {} failed",
        successful,
        cellHashes.size(),
        failed);

    return accounts;
  }

  /**
   * Searches for potential Account cells in the database by analyzing cell structure. This method
   * uses heuristics to identify cells that might contain Account data.
   *
   * @param maxResults Maximum number of candidate Account cells to return (0 for unlimited)
   * @return Set of cell hashes that are likely to contain Account data
   */
  public Set<String> findAccountCells(int maxResults) {
    Set<String> candidateHashes = new HashSet<>();
    Set<String> allCellHashes = getAllCellHashes();

    log.info("Searching for Account cells among {} total cells", allCellHashes.size());

    int processed = 0;
    int candidates = 0;

    for (String cellHash : allCellHashes) {
      if (maxResults > 0 && candidates >= maxResults) {
        break;
      }

      try {
        byte[] cellData = readCellData(cellHash);
        if (isLikelyAccountCell(cellData)) {
          candidateHashes.add(cellHash);
          candidates++;
        }
      } catch (IOException e) {
        log.debug("Error reading cell data for hash {}: {}", cellHash, e.getMessage());
      }

      processed++;
      if (processed % 100000 == 0) {
        log.info(
            "Account cell search progress: {}/{} processed, {} candidates found",
            processed,
            allCellHashes.size(),
            candidates);
      }
    }

    log.info(
        "Account cell search completed: {} candidates found from {} cells", candidates, processed);

    return candidateHashes;
  }

  /** Parses a Cell from CellDB storage format. Based on C++ vm::CellLoader implementation. */
  private Cell parseCellFromCellDbFormat(byte[] cellData) {
    try {
      if (cellData.length < 4) {
        log.debug("Cell data too short: {} bytes", cellData.length);
        return null;
      }

      // Parse refcount (first 4 bytes)
      int refcnt = ByteBuffer.wrap(cellData, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
      int dataOffset = 4;

      boolean storedAsBoc = false;
      if (refcnt == -1) {
        // Stored as BoC format
        storedAsBoc = true;
        if (cellData.length < 8) {
          log.debug("Cell data too short for BoC format: {} bytes", cellData.length);
          return null;
        }
        refcnt = ByteBuffer.wrap(cellData, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        dataOffset = 8;
      }

      if (refcnt <= 0) {
        log.debug("Invalid refcount: {}", refcnt);
        return null;
      }

      byte[] actualCellData = Arrays.copyOfRange(cellData, dataOffset, cellData.length);

      if (storedAsBoc) {
        // Parse as BoC
        return Cell.fromBoc(actualCellData);
      } else {
        // Parse as raw cell format
        return parseCellFromRawFormat(actualCellData);
      }

    } catch (Exception e) {
      log.debug("Failed to parse cell from CellDB format: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Parses a Cell from raw cell format (not BoC). This implements the cell serialization format
   * from C++ RefcntCellStorer.
   */
  private Cell parseCellFromRawFormat(byte[] rawData) {
    try {
      if (rawData.length < 2) {
        log.debug("Raw cell data too short: {} bytes", rawData.length);
        return null;
      }

      int offset = 0;

      // Parse cell descriptor (first byte)
      int flags = rawData[offset];
      int refsNum = flags & 0b111;
      boolean special = (flags & 0b1000) != 0;
      boolean withHashes = (flags & 0b10000) != 0;
      LevelMask levelMask = new LevelMask(flags >> 5);

      if (refsNum > 4) {
        throw new Error("too many refs in cell");
      }

      int ln = rawData[offset + 1] & 0xFF;
      int oneMore = ln % 2;
      int sz = (ln / 2 + oneMore);

      offset += 2;
      if ((rawData.length - offset) < sz) {
        throw new Error("failed to parse cell payload, corrupted data");
      }

      if (withHashes) {
        int maskBits = (int) Math.ceil(Math.log(levelMask.getMask() + 1) / Math.log(2));
        int hashesNum = maskBits + 1;
        offset += hashesNum * 32 + hashesNum * 2;
      }
      byte[] payload = Arrays.copyOfRange(rawData, offset, offset + sz);
      offset += sz;

      // Parse references using C++ RefcntCellParser approach
      // Each reference format: level_mask(1) + hashes(n*32) + depths(n*2)
      // where n = number of significant levels in the level mask
      Cell[] refs = new Cell[refsNum];
      for (int i = 0; i < refsNum; i++) {
        if (offset >= rawData.length) {
          throw new Error("Not enough data for reference " + i);
        }

        // Read level mask (1 byte)
        int refLevelMask = rawData[offset] & 0xFF;
        offset += 1;

        // Calculate number of significant levels
        int hashesCount = getHashesCount(refLevelMask);

        // Calculate required bytes for this reference
        int refSize = hashesCount * (32 + 2); // 32 bytes hash + 2 bytes depth per level

        if (offset + refSize > rawData.length) {
          throw new Error("Not enough data for reference " + i + " (need " + refSize + " bytes)");
        }

        // Extract hashes (32 bytes per significant level)
        byte[][] hashes = new byte[hashesCount][32];
        for (int j = 0; j < hashesCount; j++) {
          System.arraycopy(rawData, offset, hashes[j], 0, 32);
          offset += 32;
        }

        // Extract depths (2 bytes per significant level)
        int[] depths = new int[hashesCount];
        for (int j = 0; j < hashesCount; j++) {
          depths[j] = ((rawData[offset] & 0xFF) << 8) | (rawData[offset + 1] & 0xFF);
          offset += 2;
        }

        // Create external cell reference using the extracted data
        // For now, create a placeholder cell - in a full implementation,
        // you would use an ExtCellCreator to create proper external references
        refs[i] = createExternalCellReference(refLevelMask, hashes, depths);
      }

      // Verify we consumed all data
      if (offset != rawData.length) {
        log.debug(
            "Warning: {} bytes of data remaining after parsing cell", rawData.length - offset);
      }

      int bitSz = ln * 4;

      // if not full byte
      if ((ln % 2) != 0) {
        // find last bit of byte which indicates the end and cut it and next
        for (int y = 0; y < 8; y++) {
          if (((payload[payload.length - 1] >> y) & 1) == 1) {
            bitSz += 3 - y;
            break;
          }
        }
      }

      // Create cell from data with references
      CellBuilder builder = CellBuilder.beginCell();
      builder.storeBitString(new BitString(payload, bitSz));
      builder.setExotic(special);

      // Add references
      for (Cell ref : refs) {
        builder.storeRef(ref);
      }

      return builder.endCell();

    } catch (Exception e) {
      log.debug("Failed to parse raw cell format: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Calculates the number of significant levels (hashes count) from a level mask. This matches the
   * C++ LevelMask::get_hashes_count() implementation.
   */
  private int getHashesCount(int levelMask) {
    // The level mask represents which levels are significant
    // We need to count the number of set bits, but only up to level 3 (max 4 levels: 0,1,2,3)
    int count = 0;
    for (int i = 0; i <= 3; i++) { // Only check levels 0-3
      if ((levelMask & (1 << i)) != 0) {
        count++;
      }
    }
    return Math.max(1, count); // At least 1 hash is always present
  }

  /**
   * Creates an external cell reference from hash and depth data. This is a simplified
   * implementation that tries to resolve the reference by looking up the hash in the CellDB.
   */
  private Cell createExternalCellReference(int levelMask, byte[][] hashes, int[] depths) {
    try {
      if (hashes.length > 0) {
        // Try to resolve the reference by looking up the primary hash in CellDB
        byte[] primaryHash = hashes[0];
        String hashHex = Utils.bytesToHex(primaryHash);

        try {
          byte[] referencedCellData = readCellData(hashHex);
          if (referencedCellData != null) {
            // Recursively parse the referenced cell
            Cell referencedCell = parseCellFromCellDbFormat(referencedCellData);
            if (referencedCell != null) {
              return referencedCell;
            }
          }
        } catch (Exception e) {
          log.debug("Could not resolve external reference {}: {}", hashHex, e.getMessage());
        }

        // Fallback: create a simple cell with some data to avoid empty references
        return CellBuilder.beginCell()
            .storeBytes(
                Arrays.copyOf(
                    primaryHash, Math.min(primaryHash.length, 127))) // Limit to max cell size
            .endCell();
      } else {
        // Empty reference cell
        return CellBuilder.beginCell().endCell();
      }
    } catch (Exception e) {
      log.debug("Error creating external cell reference: {}", e.getMessage());
      return CellBuilder.beginCell().endCell(); // Return empty cell as fallback
    }
  }

  /**
   * Heuristic method to determine if a cell is likely to contain Account data. This uses simple
   * checks based on cell structure and size patterns.
   *
   * @param cellData Raw cell data to analyze
   * @return True if the cell might contain Account data
   */
  private boolean isLikelyAccountCell(byte[] cellData) {
    if (cellData == null || cellData.length < 10) {
      return false; // Too small to be a meaningful Account
    }

    try {
      // Parse cell header

      byte descriptor1 = cellData[0];
      byte descriptor2 = cellData[1];

      // Extract basic cell properties
      int refCount = descriptor1 & 0x07;
      boolean isExotic = (descriptor1 & 0x08) != 0;
      int dataBits = (descriptor2 & 0xFE) >> 1;

      // Account cells typically:
      // - Are not exotic cells
      // - Have reasonable data size (not too small, not too large)
      // - May have references (for storage, code, etc.)
      if (isExotic) {
        return false; // Accounts are ordinary cells
      }

      // Check data size - Accounts typically have substantial data
      if (dataBits < 8) {
        return false; // Too small or too large for typical Account
      }

      // Accounts often have references (storage, code)
      if (refCount > 4) {
        return false; // Too many references for Account
      }

      // Additional heuristic: try to parse using CellDB format and check first bit
      // Account TL-B starts with a boolean flag
      try {
        Cell cell = parseCellFromCellDbFormat(cellData);
        if (cell == null) {
          return false;
        }
        //
        CellSlice slice = CellSlice.beginParse(cell);

        // Account starts with isAccount:Bool
        // If we can read at least one bit, it's a potential Account
        if (slice.getRestBits() > 0) {
          return true;
        }
      } catch (Exception e) {
        // If CellDB parsing fails, it's probably not an Account
        return false;
      }

      return true;

    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Finds state root hashes from CellDB metadata entries. These are the root hashes that point to
   * ShardStateUnsplit cells containing account dictionaries.
   *
   * @param maxResults Maximum number of state root hashes to return (0 for unlimited)
   * @return Set of state root hashes that likely contain ShardStateUnsplit structures
   */
  public Set<String> findStateRootHashes(int maxResults) {
    Set<String> stateRootHashes = new HashSet<>();
    Map<String, Value> allEntries = getAllCellEntries();

    log.info("Searching for state root hashes among {} metadata entries", allEntries.size());

    int processed = 0;
    int candidates = 0;

    for (Value entry : allEntries.values()) {
      if (maxResults > 0 && candidates >= maxResults) {
        break;
      }

      try {
        String rootHash = entry.getRootHash();
        if (rootHash != null && !rootHash.isEmpty()) {
          // Check if this root hash points to a potential ShardStateUnsplit cell
          if (isLikelyShardStateCell(rootHash)) {
            stateRootHashes.add(rootHash);
            candidates++;
          }
        }
      } catch (Exception e) {
        log.debug("Error processing metadata entry: {}", e.getMessage());
      }

      processed++;
      if (processed % 10000 == 0) {
        log.info(
            "State root search progress: {}/{} processed, {} candidates found",
            processed,
            allEntries.size(),
            candidates);
      }
    }

    log.info(
        "State root search completed: {} candidates found from {} entries", candidates, processed);

    return stateRootHashes;
  }

  /**
   * Checks if a root hash likely points to a ShardStateUnsplit cell by attempting to parse it.
   *
   * @param rootHash The root hash to check
   * @return True if the cell is likely a ShardStateUnsplit
   */
  private boolean isLikelyShardStateCell(String rootHash) {
    try {
      byte[] cellData = readCellData(rootHash);
      if (cellData == null) {
        return false;
      }

      Cell cell = parseCellFromCellDbFormat(cellData);
      if (cell == null) {
        return false;
      }

      CellSlice slice = CellSlice.beginParse(cell);

      // Check if it starts with ShardStateUnsplit magic (0x9023afe2)
      if (slice.getRestBits() >= 32) {
        long magic = slice.preloadUint(32).longValue();
        return magic == 0x9023afe2L;
      }

      return false;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Retrieves and parses a ShardStateUnsplit structure from a root hash.
   *
   * @param stateRootHash The root hash pointing to a ShardStateUnsplit cell
   * @return The parsed ShardStateUnsplit object, or null if parsing fails
   */
  public ShardStateUnsplit getShardStateUnsplit(String stateRootHash) {
    if (stateRootHash == null || stateRootHash.isEmpty()) {
      log.debug("Invalid state root hash provided: {}", stateRootHash);
      return null;
    }

    try {
      // 1. Retrieve raw cell data
      byte[] cellData = readCellData(stateRootHash);
      if (cellData == null) {
        log.debug("No cell data found for state root hash: {}", stateRootHash);
        return null;
      }

      // 2. Parse cell from CellDB storage format
      Cell cell = parseCellFromCellDbFormat(cellData);
      if (cell == null) {
        log.debug("Failed to parse cell from CellDB format for hash: {}", stateRootHash);
        return null;
      }

      // 3. Create CellSlice for TL-B deserialization
      CellSlice cellSlice = CellSlice.beginParse(cell);

      // 4. Deserialize to ShardStateUnsplit using TL-B deserializer
      ShardStateUnsplit shardState = ShardStateUnsplit.deserialize(cellSlice);

      log.debug("Successfully parsed ShardStateUnsplit for hash: {}", stateRootHash);
      return shardState;

    } catch (Exception e) {
      log.debug("Failed to parse ShardStateUnsplit for hash {}: {}", stateRootHash, e.getMessage());
      return null;
    }
  }

  /**
   * Extracts all accounts from a ShardStateUnsplit structure using the account dictionary. This is
   * the correct approach matching the C++ implementation pattern.
   *
   * @param shardState The ShardStateUnsplit containing the account dictionary
   * @return Map of account address (BigInteger) to ShardAccount
   */
  public Map<Address, ShardAccount> extractAccountsFromShardState(ShardStateUnsplit shardState) {
    Map<Address, ShardAccount> accounts = new HashMap<>();

    if (shardState == null || shardState.getShardAccounts() == null) {
      log.debug("Invalid ShardStateUnsplit or missing ShardAccounts");
      return accounts;
    }

    try {
      ShardAccounts shardAccounts = shardState.getShardAccounts();
      List<ShardAccount> accountList = shardAccounts.getShardAccountsAsList();

      log.info("Extracting {} accounts from ShardState", accountList.size());

      for (ShardAccount shardAccount : accountList) {
        if (shardAccount != null && shardAccount.getAccount() != null) {
          accounts.put(shardAccount.getAccount().getAddress().toAddress(), shardAccount);
        }
      }

      log.info("Successfully extracted {} accounts from ShardState", accounts.size());

    } catch (Exception e) {
      log.error("Error extracting accounts from ShardState: {}", e.getMessage());
    }

    return accounts;
  }

  /**
   * Retrieves an account by its address using the proper state-based approach. This method follows
   * the C++ implementation pattern from liteserver.cpp.
   *
   * @param accountAddress The account address to lookup
   * @return The ShardAccount object, or null if not found
   */
  public ShardAccount retrieveAccountByAddress(Address accountAddress) {
    if (accountAddress == null) {
      log.debug("Invalid account address provided: null");
      return null;
    }

    log.info("Retrieving account by address: {}", accountAddress.toRaw());

    try {
      // 1. Find state root hashes
      Set<String> stateRootHashes = findStateRootHashes(10); // Limit for performance
      log.info("Found {} potential state root hashes", stateRootHashes.size());

      // 2. Try each state root to find the account
      for (String stateRootHash : stateRootHashes) {
        try {
          ShardStateUnsplit shardState = getShardStateUnsplit(stateRootHash);
          if (shardState != null) {
            Map<Address, ShardAccount> accounts = extractAccountsFromShardState(shardState);

            // 3. Look for the specific account address
            ShardAccount account = accounts.get(accountAddress);
            if (account != null) {
              log.info("Found account {} in state root {}", accountAddress.toRaw(), stateRootHash);
              return account;
            }
          }
        } catch (Exception e) {
          log.debug("Error processing state root {}: {}", stateRootHash, e.getMessage());
        }
      }

      log.info("Account {} not found in any state root", accountAddress.toRaw());
      return null;

    } catch (Exception e) {
      log.error(
          "Error retrieving account by address {}: {}", accountAddress.toRaw(), e.getMessage());
      return null;
    }
  }

  /**
   * Retrieves all accounts from all available state roots. This provides a comprehensive view of
   * all accounts stored in the CellDB.
   *
   * @param maxStateRoots Maximum number of state roots to process (0 for unlimited)
   * @return Map of account address to ShardAccount for all found accounts
   */
  public Map<Address, ShardAccount> retrieveAllAccounts(int maxStateRoots) {
    Map<Address, ShardAccount> allAccounts = new HashMap<>();

    log.info("Retrieving all accounts from CellDB state roots");

    try {
      // 1. Find all state root hashes
      Set<String> stateRootHashes = findStateRootHashes(maxStateRoots);
      log.info("Processing {} state root hashes", stateRootHashes.size());

      int processedRoots = 0;
      int totalAccounts = 0;

      // 2. Process each state root
      for (String stateRootHash : stateRootHashes) {
        try {
          ShardStateUnsplit shardState = getShardStateUnsplit(stateRootHash);
          if (shardState != null) {
            Map<Address, ShardAccount> stateAccounts = extractAccountsFromShardState(shardState);

            // 3. Merge accounts (later state roots may override earlier ones)
            allAccounts.putAll(stateAccounts);
            totalAccounts += stateAccounts.size();

            log.info(
                "Processed state root {}: {} accounts found", stateRootHash, stateAccounts.size());
          }
        } catch (Exception e) {
          log.debug("Error processing state root {}: {}", stateRootHash, e.getMessage());
        }

        processedRoots++;
        if (processedRoots % 10 == 0) {
          log.info(
              "Progress: {}/{} state roots processed, {} unique accounts found",
              processedRoots,
              stateRootHashes.size(),
              allAccounts.size());
        }
      }

      log.info(
          "Account retrieval completed: {} unique accounts from {} state roots (total {} account entries)",
          allAccounts.size(),
          processedRoots,
          totalAccounts);

    } catch (Exception e) {
      log.error("Error retrieving all accounts: {}", e.getMessage());
    }

    return allAccounts;
  }

  /** Convenience method for unlimited state root processing. */
  public Map<Address, ShardAccount> retrieveAllAccounts() {
    return retrieveAllAccounts(0);
  }

  /**
   * Enhanced statistics that include Account-related information.
   *
   * @return Map of statistic name to value, including Account statistics
   */
  public Map<String, Object> getEnhancedStatistics() {
    Map<String, Object> stats = getStatistics();

    try {
      // Add state-based account statistics
      Set<String> stateRootHashes = findStateRootHashes(5); // Sample first 5
      stats.put("state_root_hashes_sample", stateRootHashes.size());

      if (!stateRootHashes.isEmpty()) {
        // Try to parse a few ShardStateUnsplit structures
        int validStates = 0;
        int totalAccountsInSample = 0;

        for (String stateRootHash : stateRootHashes) {
          try {
            ShardStateUnsplit shardState = getShardStateUnsplit(stateRootHash);
            if (shardState != null) {
              validStates++;
              Map<Address, ShardAccount> accounts = extractAccountsFromShardState(shardState);
              totalAccountsInSample += accounts.size();
            }
          } catch (Exception e) {
            log.debug("Error processing state root for statistics: {}", e.getMessage());
          }
        }

        stats.put("valid_shard_states_sample", validStates);
        stats.put("total_accounts_in_sample", totalAccountsInSample);
      }

      // Add legacy Account-specific statistics for comparison
      Set<String> candidateAccountCells = findAccountCells(10); // Sample first 10
      stats.put("candidate_account_cells_sample", candidateAccountCells.size());

      if (!candidateAccountCells.isEmpty()) {
        // Try to retrieve a few Accounts to validate
        Set<String> sampleHashes =
            candidateAccountCells.stream().limit(10).collect(java.util.stream.Collectors.toSet());

        Map<String, Account> sampleAccounts = retrieveAccountsByHashes(sampleHashes);
        stats.put("validated_accounts_sample", sampleAccounts.size());

        // Count account states in sample
        Map<String, Integer> accountStates = new HashMap<>();
        for (Account account : sampleAccounts.values()) {
          String state = account.getAccountState();
          accountStates.put(state, accountStates.getOrDefault(state, 0) + 1);
        }
        stats.put("account_states_sample", accountStates);
      }

    } catch (Exception e) {
      log.debug("Error generating enhanced statistics: {}", e.getMessage());
      stats.put("enhanced_stats_error", e.getMessage());
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
