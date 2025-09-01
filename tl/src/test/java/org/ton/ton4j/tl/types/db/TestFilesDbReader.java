package org.ton.ton4j.tl.types.db;

import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.tl.types.db.files.GlobalIndexKey;
import org.ton.ton4j.tl.types.db.files.GlobalIndexValue;
import org.ton.ton4j.tl.types.db.files.package_.PackageValue;
import org.ton.ton4j.utils.Utils;

/**
 * Test class for the FilesDbReader that demonstrates the optimized block reading approach using the
 * Files database global index. This test shows the complete implementation of the 4-step optimized
 * workflow requested in the original task.
 */
@Slf4j
public class TestFilesDbReader {

  private static final String DB_PATH = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testGlobalIndexReading() throws IOException {

    try (FilesDbReader reader = new FilesDbReader(DB_PATH)) {

      reader
          .getGlobalIndexDb()
          .forEach(
              (key, value) -> {
                //                log.info("key: {}, value: {}", Utils.bytesToHex(key),
                // Utils.bytesToHex(value));
                GlobalIndexKey globalIndexKey = GlobalIndexKey.deserialize(key);
                GlobalIndexValue globalIndexValue = GlobalIndexValue.deserialize(value);
                log.info(
                    "globalIndexKey: {}, globalIndexValue: {}", globalIndexKey, globalIndexValue);
              });
    }
  }

  @Test
  public void testGlobalIndexReading2() throws IOException {

    try (FilesDbReader reader = new FilesDbReader(DB_PATH)) {

      reader
          .getGlobalIndexDb()
          .forEach(
              (key, value) -> {
                log.info("key: {}, value: {}", Utils.bytesToHex(key), Utils.bytesToHex(value));
              });

      for (int packageId : reader.getMainIndexIndexValue().getPackages()) {
        String packagePath = reader.getPackageFilePath(packageId);
        log.info("{} {}", packageId, packagePath);
      }
    }
  }

  /** Test the Files database structure and metadata parsing. */
  @Test
  public void testFilesDbStructure() throws IOException {
    log.info("=== Testing Files Database Structure ===");

    try (FilesDbReader reader = new FilesDbReader(DB_PATH)) {

      // Test main index loading
      log.info("Testing main index loading...");
      var mainIndex = reader.getMainIndexIndexValue();
      if (mainIndex != null) {
        log.info("Main index loaded successfully:");
        log.info("  Regular packages: {}", mainIndex.getPackages().size());
        log.info("  Key packages: {}", mainIndex.getKeyPackages().size());
        log.info("  Temp packages: {}", mainIndex.getTempPackages().size());
      } else {
        log.warn("Main index not found or empty");
      }

      // Test package metadata loading
      log.info("Testing package metadata loading...");
      Map<Long, PackageValue> packageMetadata = reader.getAllPackageMetadata();
      log.info("Loaded metadata for {} packages", packageMetadata.size());

      // Show sample package metadata
      int count = 0;
      for (Map.Entry<Long, PackageValue> entry : packageMetadata.entrySet()) {
        if (count++ < 5) {
          long packageId = entry.getKey();
          var metadata = entry.getValue();
          log.info(
              "  Package {}: key={}, temp={}, deleted={}, firstblocks={}",
              packageId,
              metadata.isKey(),
              metadata.isTemp(),
              metadata.isDeleted(),
              metadata.getFirstblocks().size());
        }
      }

      // Test package file listing
      log.info("Testing package file listing...");
      Map<Long, String> packageFiles = reader.listPackageFiles();
      log.info("Found {} package files", packageFiles.size());

      // Show sample package files
      count = 0;
      for (Map.Entry<Long, String> entry : packageFiles.entrySet()) {
        if (count++ < 5) {
          log.info("  Package file {}: {}", entry.getKey(), entry.getValue());
        }
      }

      log.info("✓ Files database structure test completed successfully");
    }
  }

  /** Test error handling and edge cases for FilesDbReader. */
  @Test
  public void testErrorHandling() throws IOException {
    log.info("=== Testing FilesDbReader Error Handling ===");

    try (FilesDbReader reader = new FilesDbReader(DB_PATH)) {

      // Test reading with invalid hash
      log.info("Testing with invalid block hash...");
      byte[] invalidResult = reader.readBlock("invalid_hash_12345");
      assert invalidResult == null : "Should return null for invalid hash";

      // Test reading with empty hash
      log.info("Testing with empty block hash...");
      byte[] emptyResult = reader.readBlock("");
      assert emptyResult == null : "Should return null for empty hash";

      // Test reading with null BlockLocation
      log.info("Testing with null BlockLocation...");
      byte[] nullLocationResult = reader.readBlockFromPackage(null);
      assert nullLocationResult == null : "Should return null for null BlockLocation";

      // Test reading with invalid BlockLocation
      log.info("Testing with invalid BlockLocation...");
      BlockLocation invalidLocation = BlockLocation.create("test", -1, -1, -1);
      byte[] invalidLocationResult = reader.readBlockFromPackage(invalidLocation);
      assert invalidLocationResult == null : "Should return null for invalid BlockLocation";

      // Test package file path with invalid package ID
      log.info("Testing package file path with invalid package ID...");
      String invalidPackagePath = reader.getPackageFilePath(-1);
      assert invalidPackagePath == null : "Should return null for invalid package ID";

      log.info("✓ Error handling test completed successfully");
    }
  }

  /** Test performance comparison between FilesDbReader and traditional approach. */
  @Test
  public void testPerformanceComparison() throws IOException {
    log.info("=== Testing Performance Comparison ===");
    log.info("Comparing FilesDbReader optimized access vs traditional sequential scanning");

    try (FilesDbReader filesReader = new FilesDbReader(DB_PATH)) {

      // Get some test hashes from Files database
      Map<String, BlockLocation> blockLocations = filesReader.getAllBlockLocations();
      if (blockLocations.isEmpty()) {
        log.warn("No blocks found in Files database for performance testing");
        return;
      }

      String[] testHashes =
          blockLocations.keySet().stream()
              .limit(5) // Test with 5 blocks
              .toArray(String[]::new);

      log.info("Testing with {} blocks from Files database", testHashes.length);

      // Test FilesDbReader optimized method
      log.info("Testing FilesDbReader optimized method...");
      long filesStartTime = System.currentTimeMillis();
      int filesSuccessCount = 0;

      for (String hash : testHashes) {
        try {
          byte[] data = filesReader.readBlock(hash);
          if (data != null) {
            filesSuccessCount++;
          }
        } catch (Exception e) {
          log.debug("FilesDbReader method failed for {}: {}", hash, e.getMessage());
        }
      }

      long filesTime = System.currentTimeMillis() - filesStartTime;

      // Test traditional method (if available)
      log.info("Testing traditional archive scanning method...");
      long traditionalTime = 0;
      int traditionalSuccessCount = 0;

      try (ArchiveDbReader archiveReader = new ArchiveDbReader(DB_PATH + "/archive")) {
        long traditionalStartTime = System.currentTimeMillis();

        for (String hash : testHashes) {
          try {
            byte[] data = archiveReader.readBlock(hash);
            if (data != null) {
              traditionalSuccessCount++;
            }
          } catch (Exception e) {
            log.debug("Traditional method failed for {}: {}", hash, e.getMessage());
          }
        }

        traditionalTime = System.currentTimeMillis() - traditionalStartTime;
      } catch (Exception e) {
        log.warn("Could not test traditional method: {}", e.getMessage());
        traditionalTime = -1; // Indicate failure
      }

      // Report results
      log.info("Performance comparison results:");
      log.info("  FilesDbReader method: {} ms, {} successful reads", filesTime, filesSuccessCount);
      if (traditionalTime >= 0) {
        log.info(
            "  Traditional method: {} ms, {} successful reads",
            traditionalTime,
            traditionalSuccessCount);
        if (traditionalTime > 0 && filesTime > 0) {
          double speedup = (double) traditionalTime / filesTime;
          log.info("  Speed improvement: {:.2f}x", speedup);
        }
      } else {
        log.info("  Traditional method: not available for comparison");
      }

      log.info("✓ Performance comparison test completed");
    }
  }

  /** Test the complete workflow from BlockLocation extraction to block parsing. */
  @Test
  public void testCompleteWorkflow() throws IOException {
    log.info("=== Testing Complete FilesDbReader Workflow ===");
    log.info("This test demonstrates the complete workflow from Files database to block data");

    try (FilesDbReader reader = new FilesDbReader(DB_PATH)) {

      // Step 1: Extract BlockLocations from Files database
      log.info("Step 1: Extracting BlockLocations from Files database global index...");
      Map<String, BlockLocation> blockLocations = reader.getAllBlockLocations();
      log.info(
          "Extracted {} BlockLocations with package location information", blockLocations.size());

      if (blockLocations.isEmpty()) {
        log.warn("No BlockLocations found - workflow test cannot proceed");
        return;
      }

      // Step 2: Analyze BlockLocation distribution
      log.info("Step 2: Analyzing BlockLocation size distribution...");
      Map<String, Integer> sizeDistribution = new java.util.HashMap<>();

      for (BlockLocation location : blockLocations.values()) {
        long size = location.getSize().longValue();
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

      log.info("BlockLocation size distribution:");
      for (Map.Entry<String, Integer> entry : sizeDistribution.entrySet()) {
        log.info("  {}: {} locations", entry.getKey(), entry.getValue());
      }

      // Step 3: Use BlockLocations to retrieve blocks
      log.info("Step 3: Using BlockLocations to retrieve block data...");
      int retrievalCount = 0;
      int successfulRetrievals = 0;
      long totalRetrievalTime = 0;

      for (Map.Entry<String, BlockLocation> entry : blockLocations.entrySet()) {
        if (retrievalCount >= 20) break; // Test first 20 BlockLocations

        String hash = entry.getKey();
        BlockLocation location = entry.getValue();
        retrievalCount++;

        try {
          long startTime = System.currentTimeMillis();
          byte[] blockData = reader.readBlockFromPackage(location);
          long endTime = System.currentTimeMillis();

          if (blockData != null) {
            successfulRetrievals++;
            totalRetrievalTime += (endTime - startTime);
            log.debug(
                "Successfully retrieved block {} (size: {} bytes, time: {} ms)",
                hash,
                blockData.length,
                (endTime - startTime));
          }
        } catch (Exception e) {
          log.debug("Error retrieving block {}: {}", hash, e.getMessage());
        }
      }

      // Step 4: Final workflow summary
      log.info("Step 4: Complete workflow summary:");
      log.info("  BlockLocations extracted: {}", blockLocations.size());
      log.info("  Retrieval attempts: {}", retrievalCount);
      log.info("  Successful retrievals: {}", successfulRetrievals);
      log.info(
          "  Success rate: {}%",
          retrievalCount > 0 ? (successfulRetrievals * 100.0) / retrievalCount : 0);
      if (successfulRetrievals > 0) {
        log.info("  Average retrieval time: {} ms", totalRetrievalTime / successfulRetrievals);
      }

      // Verify the complete workflow
      assert blockLocations.size() > 0 : "Should extract BlockLocations from Files database";
      assert successfulRetrievals > 0 : "Should successfully retrieve some blocks";

      log.info("✓ Complete FilesDbReader workflow test completed successfully");
      log.info(
          "✓ Workflow: {} BlockLocations → {} successful retrievals",
          blockLocations.size(),
          successfulRetrievals);
    }
  }
}
