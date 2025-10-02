package org.ton.ton4j.exporter.reader;

import static org.ton.ton4j.exporter.reader.CellDbReader.parseCell;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;
import org.ton.ton4j.tl.types.db.celldb.Value;
import org.ton.ton4j.tlb.ShardStateUnsplit;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class TestCellDbReader {

  private static final String TEST_DB_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testCellDbReader() throws IOException {
    RocksDbWrapper cellDb = new RocksDbWrapper(TEST_DB_PATH + "/celldb");
    cellDb.forEach(
        (key, value) -> {
          String s = new String(key);
          if (s.startsWith("desc")) {
            // meta data
            log.info("block hash: {}, value: {}", s.substring(4), Value.deserialize(value));
          } else if (s.startsWith("desczero")) {
            log.info("empty");
          } else {
            // raw cell
            log.info(
                "cell hash: {}, value (size {}): {}",
                Utils.bytesToHex(key),
                value.length,
                Utils.bytesToHex(value));
            // Cell cell = CellBuilder.beginCell().storeBytes(value).endCell();
          }
        });
    cellDb.close();
  }

  /**
   * WIP
   *
   * @throws IOException
   */
  @Test
  public void testCellDbReaderByKeyLastBlockAccountBalance() throws IOException {
    RocksDbWrapper cellDb = new RocksDbWrapper(TEST_DB_PATH + "/celldb");
    StateDbReader stateReader = new StateDbReader(TEST_DB_PATH);
    BlockIdExt last = stateReader.getLastBlockIdExt();
    log.info("last: {}", last);
    byte[] key = Utils.sha256AsArray(last.serialize());
    // construct key = "desc+base64(serialized(last))"
    String fullKey = "desc" + Utils.bytesToBase64(key);
    byte[] value = cellDb.get(fullKey.getBytes());
    log.info("key: {}, value: {}", Utils.bytesToHex(key), Utils.bytesToHex(value));

    Value shardState = Value.deserialize(ByteBuffer.wrap(value));
    log.info("shardStateValue: {}", shardState);
    byte[] shardStateRootHash = shardState.rootHash;

    // find full cell containing ShardStateUnsplit by shardStateRootHash
    byte[] rawShardStateUnsplit = cellDb.get(shardStateRootHash);
    log.info("rawShardStateUnsplit: {}", Utils.bytesToHex(rawShardStateUnsplit));

    //    rawShardStateUnsplit = Utils.slice(rawShardStateUnsplit, 6, rawShardStateUnsplit.length -
    // 6);
    //    ShardStateUnsplit shardStateUnsplit =
    //        ShardStateUnsplit.deserializeWithoutRefs(
    //            CellSlice.beginParse(Cell.fromBytesUnlimited(rawShardStateUnsplit)));
    rawShardStateUnsplit[4] = 2;
    Set<String> visited = new HashSet<>();
    Map<String, Cell> cellHash = new HashMap<>();
    Cell c = parseCell(cellDb, ByteBuffer.wrap(rawShardStateUnsplit), visited, cellHash);
    log.info("c: {}", c);

    ShardStateUnsplit shardStateUnsplit = ShardStateUnsplit.deserialize(CellSlice.beginParse(c));

    log.info("shardStateUnsplit: {}", shardStateUnsplit);
    log.info("visited: {}", visited.size());

    //    ShardStateUnsplit shardStateUnsplit =
    //        ShardStateUnsplit.deserializeWithoutRefs(CellSlice.beginParse(c));
    //    log.info("shardStateUnsplit: {}", shardStateUnsplit);

    //    ShardStateParser shardStateParser =
    //        ShardStateParser.deserialize(ByteBuffer.wrap(rawShardStateUnsplit));
    //    log.info("shardStateUnsplit: {}", shardStateParser);

    //    byte[] ref0Hash = CellSlice.beginParse(c.getRefs().get(0)).loadSignedBytes();
    //    byte[] valueOutMsgQueueInfo = cellDb.get(ref0Hash);
    //    log.info("valueOutMsgQueueInfo: {}", Utils.bytesToHex(valueOutMsgQueueInfo));
    //    OutMsgQueueInfo outMsgQueueInfo =
    //        OutMsgQueueInfo.deserialize(ByteBuffer.wrap(valueOutMsgQueueInfo));

    //    byte[] ref1Hash = CellSlice.beginParse(c.getRefs().get(1)).loadSignedBytes();
    //    byte[] valueShardAccounts = cellDb.get(ref1Hash);
    //    log.info("valueShardAccounts: {}", Utils.bytesToHex(valueShardAccounts));

    cellDb.close();
  }

  @Test
  public void testCellDbReaderBasicFunctionality() {
    try {
      log.info("Testing CellDbReader with database at: {}", TEST_DB_PATH);

      try (CellDbReader reader = new CellDbReader(TEST_DB_PATH)) {

        // Test 1: Get statistics
        log.info("=== Test 1: Getting CellDB Statistics ===");
        Map<String, Object> stats = reader.getStatistics();
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
          log.info("Stat: {} = {}", entry.getKey(), entry.getValue());
        }

        // Test 2: Get empty entry
        log.info("=== Test 2: Getting Empty Entry ===");
        Value emptyEntry = reader.getEmptyEntry();
        if (emptyEntry != null) {
          log.info(
              "Empty entry found: prev={}, next={}, rootHash={}",
              emptyEntry.getPrev(),
              emptyEntry.getNext(),
              emptyEntry.getRootHash());
        } else {
          log.warn("Empty entry not found");
        }

        // Test 3: Get all cell hashes
        log.info("=== Test 3: Getting All Cell Hashes ===");
        Set<String> cellHashes = reader.getAllCellHashes();
        log.info("Found {} cell hashes with binary data", cellHashes.size());

        // Show first few hashes as examples
        int count = 0;
        for (String hash : cellHashes) {
          if (count < 10) {
            log.info("Cell hash example {}: {}", count + 1, hash);

            // Test reading cell data for this hash
            byte[] cellData = reader.readCellData(hash);
            if (cellData != null) {
              log.info("  -> Cell data size: {} bytes", cellData.length);
            } else {
              log.info("  -> No cell data found");
            }
          }
          count++;
          if (count >= 5) break;
        }

        // Test 4: Get all cell entries (metadata)
        log.info("=== Test 4: Getting All Cell Entries (Metadata) ===");
        Map<String, Value> cellEntries = reader.getAllCellEntries();
        log.info("Found {} cell metadata entries", cellEntries.size());

        // Show first few entries as examples
        count = 0;
        for (Map.Entry<String, Value> entry : cellEntries.entrySet()) {
          if (count < 5) {
            Value value = entry.getValue();
            log.info("Cell entry {}: keyHash={}", count + 1, entry.getKey());
            if (value.getBlockId() != null) {
              log.info("  -> value : {}", value.getBlockId());
            }
            log.info("  -> RootHash: {}", value.getRootHash());
          }
          count++;
          if (count >= 5) break;
        }

        // Test 5: Test linked list traversal, works but too slow
        //        log.info("=== Test 5: Testing Linked List Traversal ===");
        //        try {
        //          List<Value> orderedEntries = reader.getAllCellEntriesOrdered();
        //          log.info("Linked list traversal found {} entries in order",
        // orderedEntries.size());
        //
        //          // Show first few ordered entries
        //          for (int i = 0; i < Math.min(3, orderedEntries.size()); i++) {
        //            Value entry = orderedEntries.get(i);
        //            if (entry.getBlockId() != null) {
        //              log.info("Ordered entry {}: workchain={}, seqno={}",
        //                       i + 1, entry.getBlockId().getWorkchain(),
        // entry.getBlockId().getSeqno());
        //            }
        //          }
        //        } catch (Exception e) {
        //          log.warn("Linked list traversal failed: {}", e.getMessage());
        //        }

        // Test 6: Test hash-to-size mappings
        log.info("=== Test 6: Testing Hash-to-Size Mappings ===");
        Map<String, Long> hashSizeMappings = reader.getAllHashSizeMappings();
        log.info("Generated {} hash-to-size mappings", hashSizeMappings.size());

        // Show some examples
        count = 0;
        for (Map.Entry<String, Long> entry : hashSizeMappings.entrySet()) {
          log.info(
              "Hash-size mapping {}: {} -> {} bytes", count + 1, entry.getKey(), entry.getValue());
          count++;
          if (count >= 3) break;
        }

        log.info("=== CellDbReader Test Completed Successfully ===");
      }

    } catch (IOException e) {
      if (e.getMessage().contains("not found")) {
        log.warn("CellDB database not found at {}, skipping test", TEST_DB_PATH);
        log.info("To run this test, ensure a CellDB database exists at the specified path");
      } else {
        log.error("Error testing CellDbReader: {}", e.getMessage(), e);
        throw new RuntimeException(e);
      }
    } catch (Exception e) {
      log.error("Unexpected error testing CellDbReader: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testCellDbReaderSpecificLookup() {
    try {
      log.info("Testing CellDbReader specific lookup functionality");

      try (CellDbReader reader = new CellDbReader(TEST_DB_PATH)) {

        // Test looking up a specific block (if we have any entries)
        Map<String, Value> allEntries = reader.getAllCellEntries();

        if (!allEntries.isEmpty()) {
          // Get the first entry to test specific lookup
          Map.Entry<String, Value> firstEntry = allEntries.entrySet().iterator().next();
          String keyHash = firstEntry.getKey();
          Value originalValue = firstEntry.getValue();

          log.info("Testing specific lookup for keyHash: {}", keyHash);

          // Test getCellEntryByHash
          Value lookedUpValue = reader.getCellEntryByHash(keyHash);
          if (lookedUpValue != null) {
            log.info("Successfully looked up entry by hash");

            // Verify the values match
            if (originalValue.getBlockId() != null && lookedUpValue.getBlockId() != null) {
              boolean matches =
                  originalValue.getBlockId().getWorkchain()
                          == lookedUpValue.getBlockId().getWorkchain()
                      && originalValue.getBlockId().getSeqno()
                          == lookedUpValue.getBlockId().getSeqno();
              log.info("Entry lookup verification: {}", matches ? "PASSED" : "FAILED");
            }
          } else {
            log.warn("Failed to look up entry by hash");
          }

          // Test getCellEntry by BlockId (if we have a valid BlockId)
          if (originalValue.getBlockId() != null) {
            Value blockLookup = reader.getCellEntry(originalValue.getBlockId());
            if (blockLookup != null) {
              log.info("Successfully looked up entry by BlockId");
            } else {
              log.warn("Failed to look up entry by BlockId");
            }
          }
        } else {
          log.info("No entries found for specific lookup test");
        }
      }

    } catch (IOException e) {
      if (e.getMessage().contains("not found")) {
        log.warn("CellDB database not found, skipping specific lookup test");
      } else {
        log.error("Error in specific lookup test: {}", e.getMessage());
      }
    }
  }
}
