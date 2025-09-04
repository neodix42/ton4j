package org.ton.ton4j.exporter.reader;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.exporter.types.CellDataAnalysis;
import org.ton.ton4j.exporter.types.CellDataInfo;
import org.ton.ton4j.tl.types.db.celldb.Value;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class TestCellDbReaderEnhanced {

  private static final String TEST_DB_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testCellDbReaderMetadataToCellDataConnection() {
    try {
      log.info("=== Enhanced CellDB Analysis: Metadata -> Cell Data Connection ===");
      log.info("Testing CellDbReader with database at: {}", TEST_DB_PATH);

      try (CellDbReader reader = new CellDbReader(TEST_DB_PATH)) {

        // Test 1: Analyze the relationship between metadata and cell data
        log.info("=== Test 1: Analyzing Metadata -> Cell Data Relationships ===");
        CellDataAnalysis analysis = reader.analyzeCellDataRelationships();
        log.info("Analysis Results: {}", analysis);
        log.info("Connection Health: {}", analysis.isHealthy() ? "HEALTHY" : "NEEDS ATTENTION");

        // Test 2: Get detailed information for first few entries
        log.info("=== Test 2: Detailed Metadata -> Cell Data Mapping ===");
        Map<String, CellDataInfo> detailedInfo = reader.getDetailedCellDataInfo(10);

        int count = 0;
        for (Map.Entry<String, CellDataInfo> entry : detailedInfo.entrySet()) {
          if (count < 5) {
            CellDataInfo info = entry.getValue();
            log.info("Entry {}: {}", count + 1, info);

            if (info.getBlockId() != null) {
              log.info(
                  "  Block: workchain={}, seqno={}",
                  info.getBlockId().getWorkchain(),
                  info.getBlockId().getSeqno());
            }

            if (info.hasCellData()) {
              log.info("  ✓ Cell data found: {} bytes", info.getCellDataSize());
            } else {
              log.info("  ✗ Cell data missing for root hash: {}", info.getRootHash());
            }
          }
          count++;
        }

        // Test 3: Demonstrate the connection using getCellDataForEntry
        log.info("=== Test 3: Direct Metadata -> Cell Data Retrieval ===");
        Map<String, Value> cellEntries = reader.getAllCellEntries();

        count = 0;
        for (Map.Entry<String, Value> entry : cellEntries.entrySet()) {
          if (count < 3) {
            Value value = entry.getValue();
            String keyHash = entry.getKey();

            log.info("Metadata Entry {}: keyHash={}", count + 1, keyHash);
            log.info("  Root Hash: {}", value.getRootHash());

            try {
              byte[] cellData = reader.getCellDataForEntry(value);
              if (cellData != null) {
                log.info("  ✓ Retrieved cell data: {} bytes", cellData.length);

                // Show first few bytes as hex
                StringBuilder hexPreview = new StringBuilder();
                for (int i = 0; i < Math.min(16, cellData.length); i++) {
                  hexPreview.append(String.format("%02x ", cellData[i] & 0xFF));
                }
                log.info("  Data preview: {}", hexPreview.toString().trim());

              } else {
                log.info("  ✗ No cell data found");
              }
            } catch (IOException e) {
              log.warn("  Error reading cell data: {}", e.getMessage());
            }
          }
          count++;
          if (count >= 3) break;
        }

        // Test 4: Enhanced statistics with relationship info
        log.info("=== Test 4: Enhanced Statistics ===");
        Map<String, Object> stats = reader.getStatistics();
        for (Map.Entry<String, Object> stat : stats.entrySet()) {
          log.info("Stat: {} = {}", stat.getKey(), stat.getValue());
        }

        // Test 5: Verify the dual storage concept
        log.info("=== Test 5: Dual Storage Verification ===");
        Set<String> cellHashes = reader.getAllCellHashes();
        Map<String, Value> metadata = reader.getAllCellEntries();

        log.info("Storage Summary:");
        log.info("  Metadata entries (desc* keys): {}", metadata.size());
        log.info("  Cell data entries (raw hash keys): {}", cellHashes.size());

        // Count how many metadata root_hashes have corresponding cell data
        int connected = 0;
        int missing = 0;

        for (Value entry : metadata.values()) {
          if (entry.getRootHash() != null) {
            if (cellHashes.contains(entry.getRootHash())) {
              connected++;
            } else {
              missing++;
            }
          }
        }

        log.info("  Connected metadata->cell data: {}", connected);
        log.info("  Missing cell data: {}", missing);
        log.info(
            "  Connection rate: {}%",
            metadata.size() > 0 ? (double) connected / metadata.size() * 100.0 : 0.0);

        log.info("=== Enhanced CellDB Analysis Completed Successfully ===");
        log.info("Key Insight: root_hash in metadata entries points to actual cell data entries");
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
  public void testCellDbReaderStorageStructureAnalysis() {
    try {
      log.info("=== CellDB Storage Structure Analysis ===");

      try (CellDbReader reader = new CellDbReader(TEST_DB_PATH)) {

        // Analyze the key patterns in the database
        log.info("=== Analyzing Database Key Patterns ===");

        Map<String, Value> metadata = reader.getAllCellEntries();
        Set<String> cellHashes = reader.getAllCellHashes();

        log.info("Database Structure Analysis:");
        log.info("1. Metadata Storage (keys starting with 'desc'):");
        log.info("   - Regular entries: 'desc' + SHA256(block_id)");
        log.info("   - Empty entry: 'desczero'");
        log.info("   - Total metadata entries: {}", metadata.size());

        log.info("2. Cell Data Storage (raw 32-byte hash keys):");
        log.info("   - Keys: raw cell hash (32 bytes)");
        log.info("   - Values: serialized cell content");
        log.info("   - Total cell data entries: {}", cellHashes.size());

        // Show examples of each storage type
        log.info("=== Storage Type Examples ===");

        // Metadata example
        if (!metadata.isEmpty()) {
          Map.Entry<String, Value> firstMetadata = metadata.entrySet().iterator().next();
          Value value = firstMetadata.getValue();

          log.info("Metadata Example:");
          log.info("  Key Hash: {}", firstMetadata.getKey());
          log.info("  Block ID: {}", value.getBlockId());
          log.info("  Root Hash: {} (points to cell data)", value.getRootHash());
          log.info("  Prev: {}", value.getPrev());
          log.info("  Next: {}", value.getNext());
        }

        // Cell data example
        if (!cellHashes.isEmpty()) {
          String firstCellHash = cellHashes.iterator().next();
          try {
            byte[] cellData = reader.readCellData(firstCellHash);
            log.info("Cell Data Example:");
            log.info("  Hash: {}", firstCellHash);
            log.info("  Data Size: {} bytes", cellData != null ? cellData.length : 0);

            //            if (cellData != null && cellData.length > 0) {
            //              StringBuilder hex = new StringBuilder();
            //              for (int i = 0; i < Math.min(32, cellData.length); i++) {
            //                hex.append(String.format("%02x", cellData[i] & 0xFF));
            //              }
            //
            //            }
            log.info("  Data: {}", Utils.bytesToHex(cellData));
          } catch (IOException e) {
            log.warn("Error reading cell data: {}", e.getMessage());
          }
        }

        log.info("=== Storage Structure Analysis Completed ===");
      }

    } catch (IOException e) {
      if (e.getMessage().contains("not found")) {
        log.warn("CellDB database not found, skipping storage structure analysis");
      } else {
        log.error("Error in storage structure analysis: {}", e.getMessage());
      }
    }
  }
}
