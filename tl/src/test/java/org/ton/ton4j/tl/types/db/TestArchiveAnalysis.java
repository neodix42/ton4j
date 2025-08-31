package org.ton.ton4j.tl.types.db;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.tlb.BlockHandle;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test class for archive analysis functionality including BlockHandle reading
 * and entry type statistics.
 */
@Slf4j
public class TestArchiveAnalysis {

  private static final String DB_PATH = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/archive";

  @Test
  public void testArchiveEntryTypeStatistics() throws IOException {
    log.info("Testing archive entry type statistics...");
    
    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {
      Map<String, Integer> stats = reader.getArchiveEntryTypeStatistics();
      
      assertNotNull("Statistics should not be null", stats);
      assertFalse("Statistics should not be empty", stats.isEmpty());
      
      log.info("Archive Entry Type Statistics:");
      for (Map.Entry<String, Integer> entry : stats.entrySet()) {
        log.info("  {}: {} entries", entry.getKey(), entry.getValue());
      }
      
      // Verify we have some expected entry types
      int totalEntries = stats.values().stream().mapToInt(Integer::intValue).sum();
      assertTrue("Should have at least some entries", totalEntries > 0);
      
      // Log summary
      log.info("Total entries analyzed: {}", totalEntries);
      log.info("Number of different entry types: {}", stats.size());
    }
  }

  @Test
  public void testBlockHandleExtractionFromIndex() throws IOException {
    log.info("Testing BlockHandle extraction from RocksDB index files...");
    
    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {
      Map<String, BlockHandle> blockHandles = reader.getAllBlockHandlesFromIndex();
      
      assertNotNull("BlockHandles should not be null", blockHandles);
      
      log.info("Found {} BlockHandles from index files", blockHandles.size());
      
      // Show some sample BlockHandles
      int count = 0;
      for (Map.Entry<String, BlockHandle> entry : blockHandles.entrySet()) {
        if (count++ < 5) {
          BlockHandle handle = entry.getValue();
          log.info("BlockHandle {}: offset={}, size={}", 
              entry.getKey(), handle.getOffset(), handle.getSize());
        }
      }
      
      // Verify BlockHandle properties
      for (Map.Entry<String, BlockHandle> entry : blockHandles.entrySet()) {
        BlockHandle handle = entry.getValue();
        assertNotNull("BlockHandle should not be null", handle);
        assertNotNull("Offset should not be null", handle.getOffset());
        assertNotNull("Size should not be null", handle.getSize());
        assertTrue("Offset should be non-negative", handle.getOffset().longValue() >= 0);
        assertTrue("Size should be positive", handle.getSize().longValue() > 0);
      }
    }
  }

  @Test
  public void testBlockHandleExtractionFromPackages() throws IOException {
    log.info("Testing BlockHandle extraction from package files (fallback)...");
    
    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {
      Map<String, BlockHandle> blockHandles = reader.getAllBlockHandlesFromPackages();
      
      assertNotNull("BlockHandles should not be null", blockHandles);
      
      log.info("Found {} BlockHandles from package files", blockHandles.size());
      
      // Show some sample BlockHandles
      int count = 0;
      for (Map.Entry<String, BlockHandle> entry : blockHandles.entrySet()) {
        if (count++ < 5) {
          BlockHandle handle = entry.getValue();
          log.info("BlockHandle {}: offset={}, size={}", 
              entry.getKey(), handle.getOffset(), handle.getSize());
        }
      }
    }
  }

  @Test
  public void testCombinedBlockHandleExtraction() throws IOException {
    log.info("Testing combined BlockHandle extraction (index + packages)...");
    
    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {
      Map<String, BlockHandle> allBlockHandles = reader.getAllBlockHandles();
      
      assertNotNull("BlockHandles should not be null", allBlockHandles);
      
      log.info("Found {} total unique BlockHandles", allBlockHandles.size());
      
      // Compare with individual methods
      Map<String, BlockHandle> indexHandles = reader.getAllBlockHandlesFromIndex();
      Map<String, BlockHandle> packageHandles = reader.getAllBlockHandlesFromPackages();
      
      log.info("Index method found: {} BlockHandles", indexHandles.size());
      log.info("Package method found: {} BlockHandles", packageHandles.size());
      log.info("Combined method found: {} BlockHandles", allBlockHandles.size());
      
      // Verify that combined >= max(index, package)
      assertTrue("Combined should have at least as many as index method", 
          allBlockHandles.size() >= indexHandles.size());
      assertTrue("Combined should have at least as many as package method", 
          allBlockHandles.size() >= packageHandles.size());
    }
  }

  @Test
  public void testBlockHandleUsageExample() throws IOException {
    log.info("Testing practical BlockHandle usage example...");
    
    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {
      Map<String, BlockHandle> blockHandles = reader.getAllBlockHandles();
      
      if (!blockHandles.isEmpty()) {
        // Demonstrate practical usage
        log.info("=== BlockHandle Usage Examples ===");
        
        // Find BlockHandles by size range
        long smallSizeThreshold = 1000;
        long largeSizeThreshold = 100000;
        
        int smallBlocks = 0;
        int mediumBlocks = 0;
        int largeBlocks = 0;
        
        for (BlockHandle handle : blockHandles.values()) {
          long size = handle.getSize().longValue();
          if (size < smallSizeThreshold) {
            smallBlocks++;
          } else if (size < largeSizeThreshold) {
            mediumBlocks++;
          } else {
            largeBlocks++;
          }
        }
        
        log.info("Block size distribution:");
        log.info("  Small blocks (< {} bytes): {}", smallSizeThreshold, smallBlocks);
        log.info("  Medium blocks ({} - {} bytes): {}", smallSizeThreshold, largeSizeThreshold, mediumBlocks);
        log.info("  Large blocks (> {} bytes): {}", largeSizeThreshold, largeBlocks);
        
        // Find BlockHandle with largest size
        BlockHandle largestHandle = blockHandles.values().stream()
            .max((h1, h2) -> h1.getSize().compareTo(h2.getSize()))
            .orElse(null);
        
        if (largestHandle != null) {
          log.info("Largest block: offset={}, size={} bytes", 
              largestHandle.getOffset(), largestHandle.getSize());
        }
        
        // Find BlockHandle with smallest size
        BlockHandle smallestHandle = blockHandles.values().stream()
            .min((h1, h2) -> h1.getSize().compareTo(h2.getSize()))
            .orElse(null);
        
        if (smallestHandle != null) {
          log.info("Smallest block: offset={}, size={} bytes", 
              smallestHandle.getOffset(), smallestHandle.getSize());
        }
        
        // Calculate total storage used
        long totalSize = blockHandles.values().stream()
            .mapToLong(h -> h.getSize().longValue())
            .sum();
        
        log.info("Total storage used by all blocks: {} bytes ({} MB)", 
            totalSize, totalSize / (1024 * 1024));
      } else {
        log.warn("No BlockHandles found for usage example");
      }
    }
  }

  @Test
  public void testArchiveDiscovery() throws IOException {
    log.info("Testing archive discovery functionality...");
    
    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {
      var archiveKeys = reader.getArchiveKeys();
      
      assertNotNull("Archive keys should not be null", archiveKeys);
      assertFalse("Should discover at least some archives", archiveKeys.isEmpty());
      
      log.info("Discovered {} archives:", archiveKeys.size());
      for (String key : archiveKeys) {
        log.info("  Archive: {}", key);
      }
      
      // Test that we can read from discovered archives
      var entries = reader.getAllEntries();
      log.info("Total entries across all archives: {}", entries.size());
      
      assertTrue("Should have some entries", entries.size() > 0);
    }
  }

  @Test
  public void testBlockHandleValidation() throws IOException {
    log.info("Testing BlockHandle validation...");
    
    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {
      Map<String, BlockHandle> blockHandles = reader.getAllBlockHandles();
      
      int validHandles = 0;
      int invalidHandles = 0;
      
      for (Map.Entry<String, BlockHandle> entry : blockHandles.entrySet()) {
        BlockHandle handle = entry.getValue();
        
        // Validate BlockHandle properties
        boolean isValid = handle != null 
            && handle.getOffset() != null 
            && handle.getSize() != null
            && handle.getOffset().longValue() >= 0
            && handle.getSize().longValue() > 0;
        
        if (isValid) {
          validHandles++;
        } else {
          invalidHandles++;
          log.warn("Invalid BlockHandle found: {}", entry.getKey());
        }
      }
      
      log.info("BlockHandle validation results:");
      log.info("  Valid handles: {}", validHandles);
      log.info("  Invalid handles: {}", invalidHandles);
      log.info("  Success rate: {:.2f}%", 
          (double) validHandles / (validHandles + invalidHandles) * 100);
      
      // Most handles should be valid
      assertTrue("Most BlockHandles should be valid", 
          validHandles > invalidHandles);
    }
  }

  @Test
  public void testIntegrationWithExistingMethods() throws IOException {
    log.info("Testing integration with existing ArchiveDbReader methods...");
    
    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {
      // Test that new methods work alongside existing ones
      var blockHandles = reader.getAllBlockHandles();
      var allEntries = reader.getAllEntries();
      var allBlocks = reader.getAllBlocks();
      var entryStats = reader.getArchiveEntryTypeStatistics();
      
      log.info("Integration test results:");
      log.info("  BlockHandles found: {}", blockHandles.size());
      log.info("  All entries found: {}", allEntries.size());
      log.info("  Blocks parsed: {}", allBlocks.size());
      log.info("  Entry types: {}", entryStats.size());
      
      // Verify consistency
      assertNotNull("BlockHandles should not be null", blockHandles);
      assertNotNull("All entries should not be null", allEntries);
      assertNotNull("All blocks should not be null", allBlocks);
      assertNotNull("Entry stats should not be null", entryStats);
      
      // BlockHandles should be a subset of or related to all entries
      // (Note: Not all entries are BlockHandles, so we can't do direct size comparison)
      assertTrue("Should have some data", 
          blockHandles.size() > 0 || allEntries.size() > 0);
    }
  }
}
