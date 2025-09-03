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
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.types.db.celldb.Value;
import org.ton.ton4j.utils.Utils;

import javax.print.attribute.ResolutionSyntax;

/**
 * Reader for TON CellDB database. The CellDB stores blockchain state cells in a RocksDB database
 * with a linked-list structure for metadata entries.
 *
 * <p>Based on the original TON C++ implementation in celldb.cpp, the CellDB uses:
 * 1. Metadata entries: key = "desc" + SHA256(TL-serialized block_id), value = TL-serialized db.celldb.value
 * 2. Special empty entry: key = "desczero", value = TL-serialized db.celldb.value (sentinel for linked list)
 * 3. Cell data entries: key = cell hash (raw bytes), value = serialized cell content
 * 4. Linked list structure: entries connected via prev/next pointers forming a doubly-linked list
 *
 * <p>The TL types used:
 * - db.celldb.value: block_id:tonNode.blockIdExt prev:int256 next:int256 root_hash:int256
 * - db.celldb.key.value: hash:int256
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
        emptyEntry = Value.deserialize(buffer);
        log.debug("Loaded empty entry: prev={}, next={}", 
                  emptyEntry.getPrev(),
                  emptyEntry.getNext());
      } else {
        log.warn("Empty entry not found in CellDB");
        // Create a placeholder empty entry
        emptyEntry = Value.builder()
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
   * Gets all cell entries from the CellDB as a map of key hash to Value.
   * This method scans all metadata entries (keys starting with "desc").
   *
   * @return Map of key hash to CellDB Value
   */
  public Map<String, Value> getAllCellEntries() {
    Map<String, Value> cellEntries = new HashMap<>();

    log.info("Reading all cell entries from CellDB...");

    AtomicInteger totalEntries = new AtomicInteger(0);
    AtomicInteger validEntries = new AtomicInteger(0);
    AtomicInteger parseErrors = new AtomicInteger(0);

    cellDb.forEach((key, value) -> {
      try {
        totalEntries.incrementAndGet();
        String keyStr = new String(key);

        // Skip non-metadata keys
        if (!keyStr.startsWith("desc")) {
          return;
        }

        // Parse the TL-serialized value
        ByteBuffer buffer = ByteBuffer.wrap(value);
        Value cellValue = Value.deserialize(buffer);

        // Extract key hash from the key (remove "desc" prefix for regular entries)
        String keyHash;
        if ("desczero".equals(keyStr)) {
          keyHash = ""; // Empty key hash for sentinel entry
        } else {
          keyHash = keyStr.substring(4); // Remove "desc" prefix
        }

        cellEntries.put(keyHash, cellValue);
        entryCache.put(keyHash, cellValue);
        validEntries.incrementAndGet();

        if (validEntries.get() % 1000 == 0) {
          log.debug("Processed {} cell entries", validEntries.get());
        }

      } catch (Exception e) {
        parseErrors.incrementAndGet();
        log.debug("Error processing CellDB entry: {}", e.getMessage());
      }
    });

    log.info("CellDB parsing: {} total entries, {} valid cell entries, {} parse errors",
             totalEntries.get(), validEntries.get(), parseErrors.get());

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
   * Gets all cell entries in linked list order by traversing the prev/next pointers.
   * This follows the C++ implementation logic for ordered iteration.
   *
   * @return List of CellDB Values in linked list order
   */
  public List<Value> getAllCellEntriesOrdered() {
    List<Value> orderedEntries = new ArrayList<>();

    if (emptyEntry == null) {
      log.warn("Empty entry not available, cannot traverse linked list");
      return orderedEntries;
    }

    try {
      // Start from the first real entry (next of empty entry)
      String currentHash = emptyEntry.getNext();
      Set<String> visited = new HashSet<>();

      while (!currentHash.equals("0000000000000000000000000000000000000000000000000000000000000000") 
             && !visited.contains(currentHash)) {
        
        visited.add(currentHash);
        Value currentEntry = getCellEntryByHash(currentHash);

        if (currentEntry == null) {
          log.warn("Broken linked list: entry {} not found", currentHash);
          break;
        }

        // Check if this is the empty entry (end of list)
        if (isEmptyBlockId(currentEntry.getBlockId())) {
          break;
        }

        orderedEntries.add(currentEntry);

        // Move to next entry
        currentHash = currentEntry.getNext();

        // Prevent infinite loops
        if (orderedEntries.size() > 1000000) {
          log.warn("Linked list traversal stopped: too many entries (possible loop)");
          break;
        }
      }

      log.info("Traversed linked list: {} entries in order", orderedEntries.size());

    } catch (Exception e) {
      log.error("Error traversing linked list: {}", e.getMessage());
    }

    return orderedEntries;
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
   * Gets all cell hashes that have binary data stored in the database.
   * This scans for raw hash keys (32 bytes) that are not metadata entries.
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
   * Gets hash-to-offset mappings for consistency with other readers.
   * Note: CellDB doesn't use offset-based storage like archive packages,
   * so this returns hash-to-size mappings instead.
   *
   * @return Map of hash to data size
   */
  public Map<String, Long> getAllHashOffsetMappings() {
    Map<String, Long> hashSizeMappings = new HashMap<>();

    cellDb.forEach((key, value) -> {
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
   * Generates the database key for a given key hash.
   * Following C++ implementation: "desc" + keyHash for regular entries.
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
   * Generates the key hash for a block ID by SHA256 hashing the TL-serialized block ID.
   * This follows the C++ implementation logic.
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
   * Creates an empty block ID for the sentinel entry.
   *
   * @return Empty block ID
   */
  private static BlockIdExt getEmptyBlockId() {
    return BlockIdExt.builder()
        .workchain(-1)  // Invalid workchain
        .shard(0)
        .seqno(0)
        .rootHash(new byte[32])
        .fileHash(new byte[32])
        .build();
  }

  /**
   * Checks if a block ID represents the empty/sentinel entry.
   *
   * @param blockId The block ID to check
   * @return True if this is an empty block ID
   */
  private static boolean isEmptyBlockId(BlockIdExt blockId) {
    if (blockId == null) {
      return true;
    }
    
    // Check for invalid workchain (sentinel marker)
    return blockId.getWorkchain() == -1 && blockId.getSeqno() == 0;
  }

  /**
   * Checks if a block ID is valid (not null and has reasonable values).
   *
   * @param blockId The block ID to check
   * @return True if valid
   */
  private static boolean isValidBlockId(BlockIdExt blockId) {
    return blockId != null && 
           blockId.getWorkchain() != -1 && 
           blockId.getRootHash() != null && 
           blockId.getFileHash() != null;
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
