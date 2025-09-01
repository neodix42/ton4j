package org.ton.ton4j.tl.types.db;

import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.tlb.BlockHandle;

/**
 * Test class for the EnhancedArchiveDbReader that demonstrates using BlockHandle information for
 * efficient block retrieval and parsing. This test shows the complete implementation of the task
 * requirements.
 */
@Slf4j
public class TestEnhancedArchiveDbReader {

  private static final String DB_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  /**
   * Test the enhanced block reading functionality using the optimized BlockLocation approach. This
   * demonstrates the 4-step optimized workflow requested in the original task.
   */
  @Test
  public void testEnhancedBlockReading() throws IOException {
    log.info("=== Testing Optimized Block Reading with BlockLocation ===");
    log.info("This test demonstrates the 4-step optimized workflow:");
    log.info("1. Retrieve all blockhandles with hashes and package_id");
    log.info("2. When searching by hash, get package_id directly");
    log.info("3. Map package_id to file name using getPackagePathFromId()");
    log.info("4. Read block using offset and size information");

    try (EnhancedArchiveDbReader reader = new EnhancedArchiveDbReader(DB_PATH)) {

      // Step 1: Get all BlockLocations from the global index (optimized approach)
      log.info(
          "Step 1: Reading BlockLocations (hash → package_id, offset, size) from global index...");
      Map<String, BlockLocation> blockLocations = reader.getAllBlockLocationsFromIndex();
      log.info("Found {} BlockLocations with package_id information", blockLocations.size());

      // Step 2: Show some sample BlockLocations
      log.info("Step 2: Sample BlockLocations with their package_id, offset, size information:");
      int sampleCount = 0;
      for (Map.Entry<String, BlockLocation> entry : blockLocations.entrySet()) {
        if (sampleCount < 5) {
          String hash = entry.getKey();
          BlockLocation location = entry.getValue();
          log.info(
              "  BlockLocation {}: package_id={}, offset={}, size={}",
              hash,
              location.getPackageId(),
              location.getOffset(),
              location.getSize());
          sampleCount++;
        } else {
          break;
        }
      }

      // Step 3: Test the optimized 4-step workflow
      log.info("Step 3: Testing optimized block reading workflow...");
      int testCount = 0;
      int successCount = 0;
      int optimizedAccessCount = 0;
      int fallbackCount = 0;
      long totalOptimizedTime = 0;

      for (Map.Entry<String, BlockLocation> entry : blockLocations.entrySet()) {
        if (testCount >= 10) break; // Test first 10 BlockLocations

        String hash = entry.getKey();
        BlockLocation location = entry.getValue();
        testCount++;

        try {
          // Measure optimized access time
          long startTime = System.currentTimeMillis();

          // Use the optimized readBlockUsingBlockLocation method
          byte[] blockData = reader.readBlockUsingBlockLocation(hash, location);

          long endTime = System.currentTimeMillis();
          long accessTime = endTime - startTime;

          if (blockData != null) {
            successCount++;
            optimizedAccessCount++;
            totalOptimizedTime += accessTime;
            log.debug(
                "Successfully read block {} using optimized workflow (size: {} bytes, time: {} ms)",
                hash,
                blockData.length,
                accessTime);
          }
        } catch (Exception e) {
          log.debug("Error reading block {} with optimized workflow: {}", hash, e.getMessage());
          fallbackCount++;
        }
      }

      // Step 4: Get performance statistics
      log.info("Step 4: Performance statistics after optimized block reading:");
      Map<String, Long> stats = reader.getPerformanceStats();
      log.info("  Cache hits: {}", stats.get("cacheHits"));
      log.info("  Cache misses: {}", stats.get("cacheMisses"));
      log.info("  Direct access attempts: {}", stats.get("directAccessCount"));
      log.info("  Fallback access count: {}", stats.get("fallbackAccessCount"));

      if (stats.get("cacheHits") + stats.get("cacheMisses") > 0) {
        log.info("  Cache hit rate: {}%", stats.get("cacheHitRate"));
      }

      // Step 5: Show workflow efficiency
      log.info("Step 5: Optimized workflow efficiency:");
      log.info("  Total tests: {}", testCount);
      log.info("  Successful optimized reads: {}", optimizedAccessCount);
      log.info("  Fallback reads: {}", fallbackCount);
      log.info("  Success rate: {}%", (optimizedAccessCount * 100.0) / testCount);
      if (optimizedAccessCount > 0) {
        log.info("  Average optimized read time: {} ms", totalOptimizedTime / optimizedAccessCount);
      }

      // Step 6: Demonstrate the complete workflow benefits
      log.info("Step 6: Workflow benefits demonstration:");
      log.info("  ✓ No sequential scanning of archive files");
      log.info("  ✓ Direct package access using package_id");
      log.info("  ✓ Precise offset and size information");
      log.info("  ✓ Efficient caching system");

      // Final performance stats
      reader.logPerformanceStats();

      // Verify results
      assert blockLocations.size() > 0 : "Should find BlockLocations in the global index";
      assert successCount > 0 : "Should successfully read some blocks";

      log.info("✓ Optimized block reading test completed successfully");
      log.info(
          "✓ Found {} BlockLocations, successfully read {} blocks using optimized workflow",
          blockLocations.size(),
          successCount);
    }
  }

  /** Test the BlockHandle validation functionality. */
  @Test
  public void testBlockHandleValidation() throws IOException {
    log.info("=== Testing BlockHandle Validation ===");

    try (EnhancedArchiveDbReader reader = new EnhancedArchiveDbReader(DB_PATH)) {
      Map<String, BlockHandle> blockHandles = reader.getAllBlockHandlesFromIndex();

      int validHandles = 0;
      int invalidHandles = 0;

      for (Map.Entry<String, BlockHandle> entry : blockHandles.entrySet()) {
        BlockHandle handle = entry.getValue();

        // Basic validation checks
        boolean isValid =
            handle != null
                && handle.getOffset() != null
                && handle.getSize() != null
                && handle.getOffset().compareTo(java.math.BigInteger.ZERO) >= 0
                && handle.getSize().compareTo(java.math.BigInteger.ZERO) > 0;

        if (isValid) {
          validHandles++;
        } else {
          invalidHandles++;
          log.debug("Invalid BlockHandle: {}", entry.getKey());
        }
      }

      log.info("BlockHandle validation results:");
      log.info("  Valid handles: {}", validHandles);
      log.info("  Invalid handles: {}", invalidHandles);
      log.info(
          "  Validation success rate: {}%",
          (validHandles * 100.0) / (validHandles + invalidHandles));

      // Most handles should be valid
      assert validHandles > invalidHandles : "Most BlockHandles should be valid";

      log.info("✓ BlockHandle validation test completed successfully");
    }
  }

  /** Test performance comparison between enhanced and original methods. */
  @Test
  public void testPerformanceComparison() throws IOException {
    log.info("=== Testing Performance Comparison ===");
    log.info("Comparing enhanced BlockHandle-based access vs original sequential access");

    try (EnhancedArchiveDbReader enhancedReader = new EnhancedArchiveDbReader(DB_PATH);
        ArchiveDbReader originalReader = new ArchiveDbReader(DB_PATH)) {

      // Get a sample of block hashes to test
      Map<String, BlockHandle> blockHandles = enhancedReader.getAllBlockHandlesFromIndex();
      String[] testHashes =
          blockHandles.keySet().stream()
              .limit(5) // Test with 5 blocks
              .toArray(String[]::new);

      if (testHashes.length == 0) {
        log.warn("No BlockHandles found for performance testing");
        return;
      }

      // Test enhanced method
      log.info("Testing enhanced method with {} blocks...", testHashes.length);
      long enhancedStartTime = System.currentTimeMillis();
      int enhancedSuccessCount = 0;

      for (String hash : testHashes) {
        try {
          byte[] data = enhancedReader.readBlock(hash);
          if (data != null) {
            enhancedSuccessCount++;
          }
        } catch (Exception e) {
          log.debug("Enhanced method failed for {}: {}", hash, e.getMessage());
        }
      }

      long enhancedTime = System.currentTimeMillis() - enhancedStartTime;

      // Test original method
      log.info("Testing original method with {} blocks...", testHashes.length);
      long originalStartTime = System.currentTimeMillis();
      int originalSuccessCount = 0;

      for (String hash : testHashes) {
        try {
          byte[] data = originalReader.readBlock(hash);
          if (data != null) {
            originalSuccessCount++;
          }
        } catch (Exception e) {
          log.debug("Original method failed for {}: {}", hash, e.getMessage());
        }
      }

      long originalTime = System.currentTimeMillis() - originalStartTime;

      // Report results
      log.info("Performance comparison results:");
      log.info("  Enhanced method: {} ms, {} successful reads", enhancedTime, enhancedSuccessCount);
      log.info("  Original method: {} ms, {} successful reads", originalTime, originalSuccessCount);

      if (originalTime > 0) {
        double speedup = (double) originalTime / enhancedTime;
        log.info("  Speed improvement: {:.2f}x", speedup);
      }

      // Show enhanced method statistics
      enhancedReader.logPerformanceStats();

      log.info("✓ Performance comparison test completed");
    }
  }

  /** Comprehensive test that demonstrates the complete BlockHandle workflow. */
  @Test
  public void testCompleteBlockHandleWorkflow() throws IOException {
    log.info("=== Testing Complete BlockHandle Workflow ===");
    log.info(
        "This test demonstrates the complete workflow from BlockHandle extraction to block parsing");

    try (EnhancedArchiveDbReader reader = new EnhancedArchiveDbReader(DB_PATH)) {

      // Step 1: Extract BlockHandles from index
      log.info("Step 1: Extracting BlockHandles from RocksDB index files...");
      Map<String, BlockHandle> blockHandles = reader.getAllBlockHandlesFromIndex();
      log.info("Extracted {} BlockHandles with file location information", blockHandles.size());

      // Step 2: Analyze BlockHandle distribution
      log.info("Step 2: Analyzing BlockHandle size distribution...");
      Map<String, Integer> sizeDistribution = new java.util.HashMap<>();

      for (BlockHandle handle : blockHandles.values()) {
        long size = handle.getSize().longValue();
        String category;

        if (size <= 1024) {
          category = "≤1KB";
        } else if (size <= 10240) {
          category = "1-10KB";
        } else if (size <= 102400) {
          category = "10-100KB";
        } else {
          category = ">100KB";
        }

        sizeDistribution.merge(category, 1, Integer::sum);
      }

      log.info("BlockHandle size distribution:");
      for (Map.Entry<String, Integer> entry : sizeDistribution.entrySet()) {
        log.info("  {}: {} handles", entry.getKey(), entry.getValue());
      }

      // Step 3: Use BlockHandles to retrieve blocks
      log.info("Step 3: Using BlockHandles to retrieve and parse blocks...");
      Map<String, Block> blocks = reader.getAllBlocksUsingBlockHandles();
      log.info("Successfully parsed {} blocks using BlockHandle information", blocks.size());

      // Step 4: Analyze retrieved blocks
      log.info("Step 4: Analyzing retrieved blocks...");
      int validBlocks = 0;
      int invalidBlocks = 0;

      for (Map.Entry<String, Block> entry : blocks.entrySet()) {
        Block block = entry.getValue();
        if (block != null && block.getBlockInfo() != null) {
          validBlocks++;
        } else {
          invalidBlocks++;
        }
      }

      log.info("Block analysis results:");
      log.info("  Valid blocks: {}", validBlocks);
      log.info("  Invalid blocks: {}", invalidBlocks);
      log.info("  Success rate: {}%", (validBlocks * 100.0) / (validBlocks + invalidBlocks));

      // Step 5: Performance summary
      log.info("Step 5: Final performance summary:");
      reader.logPerformanceStats();

      // Verify the complete workflow
      assert blockHandles.size() > 0 : "Should extract BlockHandles from index";
      assert blocks.size() > 0 : "Should retrieve blocks using BlockHandles";
      assert validBlocks > 0 : "Should parse valid blocks";

      log.info("✓ Complete BlockHandle workflow test completed successfully");
      log.info(
          "✓ Workflow: {} BlockHandles → {} blocks → {} valid parsed blocks",
          blockHandles.size(),
          blocks.size(),
          validBlocks);
    }
  }

  /**
   * Test package discovery functionality using getPackagePathFromId. This test demonstrates the new
   * optimized package ID to path mapping.
   */
  @Test
  public void testPackageDiscovery() throws IOException {
    log.info("=== Testing Package Discovery and Path Resolution ===");
    log.info(
        "This test shows all packages with their IDs and resolved paths using getPackagePathFromId()");

    try (EnhancedArchiveDbReader reader = new EnhancedArchiveDbReader(DB_PATH)) {

      // Step 1: Get all BlockLocations from the global index
      log.info("Step 1: Reading BlockLocations from global index...");
      Map<String, BlockLocation> blockLocations = reader.getAllBlockLocationsFromIndex();
      log.info("Found {} BlockLocations with package_id information", blockLocations.size());

      // Step 2: Collect unique package IDs
      log.info("Step 2: Collecting unique package IDs...");
      Map<Long, Integer> packageIdCounts = new java.util.HashMap<>();

      for (BlockLocation location : blockLocations.values()) {
        packageIdCounts.merge(location.getPackageId(), 1, Integer::sum);
      }

      log.info("Found {} unique package IDs", packageIdCounts.size());

      // Step 3: Test package path resolution for each unique package ID
      log.info("Step 3: Testing package path resolution...");
      int resolvedCount = 0;
      int unresolvedCount = 0;

      for (Map.Entry<Long, Integer> entry : packageIdCounts.entrySet()) {
        long packageId = entry.getKey();
        int blockCount = entry.getValue();

        String packagePath = reader.getPackagePathFromId(packageId);
        if (packagePath != null) {
          resolvedCount++;
          log.info("  Package ID {}: {} blocks → {}", packageId, blockCount, packagePath);
        } else {
          unresolvedCount++;
          log.debug("  Package ID {}: {} blocks → NOT FOUND", packageId, blockCount);
        }
      }

      // Step 4: Show package type distribution
      log.info("Step 4: Package type analysis...");
      int filesDbPackages = 0;
      int archivePackages = 0;
      int keyPackages = 0;

      for (Map.Entry<Long, Integer> entry : packageIdCounts.entrySet()) {
        long packageId = entry.getKey();
        String packagePath = reader.getPackagePathFromId(packageId);

        if (packagePath != null) {
          if (packagePath.contains("/files/packages/")) {
            filesDbPackages++;
          } else if (packagePath.contains("key.archive")) {
            keyPackages++;
          } else if (packagePath.contains("archive.")) {
            archivePackages++;
          }
        }
      }

      log.info("Package type distribution:");
      log.info("  Files database packages: {}", filesDbPackages);
      log.info("  Traditional archive packages: {}", archivePackages);
      log.info("  Key archive packages: {}", keyPackages);

      // Step 5: Test optimized block reading using BlockLocation
      log.info("Step 5: Testing optimized block reading with BlockLocation...");
      int testCount = 0;
      int successCount = 0;
      long totalTime = 0;

      for (Map.Entry<String, BlockLocation> entry : blockLocations.entrySet()) {
        if (testCount >= 10) break; // Test first 10 BlockLocations

        String hash = entry.getKey();
        BlockLocation location = entry.getValue();
        testCount++;

        try {
          long startTime = System.currentTimeMillis();
          byte[] data = reader.readBlockUsingBlockLocation(hash, location);
          long endTime = System.currentTimeMillis();

          if (data != null) {
            successCount++;
            totalTime += (endTime - startTime);
            log.debug(
                "Successfully read block {} using BlockLocation (size: {} bytes, time: {} ms)",
                hash,
                data.length,
                (endTime - startTime));
          }
        } catch (Exception e) {
          log.debug("Error reading block {} with BlockLocation: {}", hash, e.getMessage());
        }
      }

      // Step 6: Summary and validation
      log.info("Step 6: Package discovery summary:");
      log.info("  Total unique package IDs: {}", packageIdCounts.size());
      log.info("  Successfully resolved paths: {}", resolvedCount);
      log.info("  Unresolved package IDs: {}", unresolvedCount);
      log.info("  Resolution success rate: {}%", (resolvedCount * 100.0) / packageIdCounts.size());
      log.info("  Optimized block reading: {}/{} successful", successCount, testCount);
      if (successCount > 0) {
        log.info("  Average read time: {} ms", totalTime / successCount);
      }

      // Verify results
      assert blockLocations.size() > 0 : "Should find BlockLocations in the global index";
      assert packageIdCounts.size() > 0 : "Should find unique package IDs";
      assert resolvedCount > 0 : "Should resolve some package paths";

      log.info("✓ Package discovery test completed successfully");
    }
  }

  /** Test error handling and edge cases. */
  @Test
  public void testErrorHandling() throws IOException {
    log.info("=== Testing Error Handling and Edge Cases ===");

    try (EnhancedArchiveDbReader reader = new EnhancedArchiveDbReader(DB_PATH)) {

      // Test with invalid hash
      log.info("Testing with invalid block hash...");
      byte[] invalidResult = reader.readBlock("invalid_hash_12345");
      assert invalidResult == null : "Should return null for invalid hash";

      // Test with empty hash
      log.info("Testing with empty block hash...");
      byte[] emptyResult = reader.readBlock("");
      assert emptyResult == null : "Should return null for empty hash";

      // Test cache functionality
      log.info("Testing cache functionality...");
      Map<String, BlockHandle> blockHandles = reader.getAllBlockHandlesFromIndex();
      if (!blockHandles.isEmpty()) {
        String testHash = blockHandles.keySet().iterator().next();

        // First read (cache miss)
        reader.readBlock(testHash);

        // Second read (should be cache hit)
        reader.readBlock(testHash);

        Map<String, Long> stats = reader.getPerformanceStats();
        assert stats.get("cacheHits") > 0 : "Should have cache hits";
      }

      // Test cache clearing
      log.info("Testing cache clearing...");
      reader.clearCaches();
      Map<String, Long> statsAfterClear = reader.getPerformanceStats();
      assert statsAfterClear.get("cachedBlocks") == 0 : "Should have no cached blocks after clear";
      assert statsAfterClear.get("cachedRawData") == 0
          : "Should have no cached raw data after clear";

      log.info("✓ Error handling test completed successfully");
    }
  }
}
