package org.ton.ton4j.tl.types.db;

import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.tlb.BlockHandle;

/**
 * Test class for analyzing TON archive database entries and BlockHandles. This test demonstrates
 * the complete functionality requested in the original task.
 */
@Slf4j
public class TestArchiveEntryAnalysis {

  private static final String DB_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/archive";

  /**
   * Test method that shows how many entries/blocks are in archive packages grouped by their types
   * (Block, BlockProof, BlockHandle) as requested in the original task.
   */
  @Test
  public void testArchiveEntryTypeStatistics() throws IOException {
    log.info("=== Testing Archive Entry Type Statistics ===");
    log.info("Analyzing TON archive database to count entries by type...");

    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {
      // Get statistics about entry types grouped by Block, BlockProof, and BlockHandle
      Map<String, Integer> stats = reader.getArchiveEntryTypeStatistics();

      // Display the results
      log.info("=== ARCHIVE ENTRY TYPE STATISTICS ===");
      log.info("Block entries: {}", stats.getOrDefault("Block", 0));
      log.info("BlockProof entries: {}", stats.getOrDefault("BlockProof", 0));
      log.info("BlockHandle entries: {}", stats.getOrDefault("BlockHandle", 0));
      log.info("Other entries: {}", stats.getOrDefault("Other", 0));

      int totalEntries = stats.values().stream().mapToInt(Integer::intValue).sum();
      log.info("Total entries analyzed: {}", totalEntries);

      // Calculate percentages
      if (totalEntries > 0) {
        log.info("=== PERCENTAGE BREAKDOWN ===");
        log.info("Blocks: {}%", (stats.getOrDefault("Block", 0) * 100.0) / totalEntries);
        log.info("BlockProofs: {}%", (stats.getOrDefault("BlockProof", 0) * 100.0) / totalEntries);
        log.info(
            "BlockHandles: {}%", (stats.getOrDefault("BlockHandle", 0) * 100.0) / totalEntries);
        log.info("Other: {}%", (stats.getOrDefault("Other", 0) * 100.0) / totalEntries);
      }

      // Verify that we found some entries
      assert totalEntries > 0 : "Should find at least some entries in the archive database";
      assert stats.getOrDefault("BlockHandle", 0) > 0
          : "Should find at least some BlockHandle entries";

      log.info("✓ Archive entry type statistics test completed successfully");
    }
  }

  /**
   * Test method that reads all BlockHandles from the archive database as requested in the original
   * task. This uses the BlockHandle class from
   * /home/neodix/gitProjects/ton4j/tlb/src/main/java/org/ton/ton4j/tlb/BlockHandle.java
   */
  @Test
  public void testReadAllBlockHandles() throws IOException {
    log.info("=== Testing Read All BlockHandles ===");
    log.info("Reading all BlockHandles from TON archive database...");

    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {
      // Read all BlockHandles using the primary method (from RocksDB indexes)
      Map<String, BlockHandle> blockHandles = reader.getAllBlockHandlesFromIndex();

      log.info("=== BLOCKHANDLE READING RESULTS ===");
      log.info("Total BlockHandles found: {}", blockHandles.size());

      // Display first few BlockHandles as examples
      int count = 0;
      for (Map.Entry<String, BlockHandle> entry : blockHandles.entrySet()) {
        if (count < 10) { // Show first 10 as examples
          String blockId = entry.getKey();
          BlockHandle handle = entry.getValue();
          log.info(
              "BlockHandle {}: offset={}, size={}", blockId, handle.getOffset(), handle.getSize());
          count++;
        } else {
          break;
        }
      }

      if (blockHandles.size() > 10) {
        log.info("... and {} more BlockHandles", blockHandles.size() - 10);
      }

      // Verify that we found BlockHandles
      assert blockHandles.size() > 0
          : "Should find at least some BlockHandles in the archive database";

      // Test the comprehensive method that combines both index and package reading
      log.info("=== Testing Comprehensive BlockHandle Reading ===");
      Map<String, BlockHandle> allBlockHandles = reader.getAllBlockHandles();
      log.info("Total BlockHandles (comprehensive method): {}", allBlockHandles.size());

      // Verify comprehensive method finds at least as many as the index method
      assert allBlockHandles.size() >= blockHandles.size()
          : "Comprehensive method should find at least as many BlockHandles as index method";

      log.info("✓ BlockHandle reading test completed successfully");
    }
  }

  /** Combined test that demonstrates both functionalities together. */
  @Test
  public void testCompleteArchiveAnalysis() throws IOException {
    log.info("=== Testing Complete Archive Analysis ===");
    log.info("Performing comprehensive analysis of TON archive database...");

    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {
      // 1. Get entry type statistics
      log.info("Step 1: Analyzing entry types...");
      Map<String, Integer> stats = reader.getArchiveEntryTypeStatistics();

      // 2. Read all BlockHandles
      log.info("Step 2: Reading all BlockHandles...");
      Map<String, BlockHandle> blockHandles = reader.getAllBlockHandles();

      // 3. Display comprehensive results
      log.info("=== COMPREHENSIVE ARCHIVE ANALYSIS RESULTS ===");

      log.info("ENTRY TYPE DISTRIBUTION:");
      log.info("  - Blocks: {}", stats.getOrDefault("Block", 0));
      log.info("  - BlockProofs: {}", stats.getOrDefault("BlockProof", 0));
      log.info("  - BlockHandles: {}", stats.getOrDefault("BlockHandle", 0));
      log.info("  - Other: {}", stats.getOrDefault("Other", 0));

      log.info("BLOCKHANDLE ANALYSIS:");
      log.info("  - Total BlockHandles extracted: {}", blockHandles.size());

      // Analyze BlockHandle size distribution
      Map<String, Integer> sizeDistribution = new java.util.HashMap<>();
      for (BlockHandle handle : blockHandles.values()) {
        String sizeCategory;
        long size = handle.getSize().longValue();
        if (size <= 1024) {
          sizeCategory = "≤1KB";
        } else if (size <= 10240) {
          sizeCategory = "1-10KB";
        } else if (size <= 102400) {
          sizeCategory = "10-100KB";
        } else {
          sizeCategory = ">100KB";
        }
        sizeDistribution.merge(sizeCategory, 1, Integer::sum);
      }

      log.info("BLOCKHANDLE SIZE DISTRIBUTION:");
      for (Map.Entry<String, Integer> entry : sizeDistribution.entrySet()) {
        log.info("  - {}: {} handles", entry.getKey(), entry.getValue());
      }

      // Show sample BlockHandles with their details
      log.info("SAMPLE BLOCKHANDLES:");
      int sampleCount = 0;
      for (Map.Entry<String, BlockHandle> entry : blockHandles.entrySet()) {
        if (sampleCount < 5) {
          String blockId = entry.getKey();
          BlockHandle handle = entry.getValue();
          log.info("  - BlockID: {}", blockId);
          log.info("    Offset: {}, Size: {}", handle.getOffset(), handle.getSize());
          sampleCount++;
        } else {
          break;
        }
      }

      // Verify results
      int totalEntries = stats.values().stream().mapToInt(Integer::intValue).sum();
      assert totalEntries > 0 : "Should find entries in the archive database";
      assert blockHandles.size() > 0 : "Should find BlockHandles in the archive database";
      assert stats.getOrDefault("BlockHandle", 0) > 0
          : "Should find BlockHandle entries in statistics";

      log.info("✓ Complete archive analysis test completed successfully");
      log.info("✓ Found {} total entries with {} BlockHandles", totalEntries, blockHandles.size());
    }
  }
}
