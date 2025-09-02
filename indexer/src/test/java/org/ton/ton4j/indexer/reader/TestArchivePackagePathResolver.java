package org.ton.ton4j.indexer.reader;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * Test class for the archive package path resolution functionality in GlobalIndexDbReader. This
 * test demonstrates how to resolve package IDs from the global index to their actual archive
 * package file paths in the directory structure.
 */
@Slf4j
public class TestArchivePackagePathResolver {

  private static final String DB_PATH = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testArchivePackagePathResolution() throws IOException {
    log.info("=== Testing Archive Package Path Resolution ===");

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {

      // Get the main index to see available package IDs
      var mainIndex = reader.getMainIndexIndexValue();
      if (mainIndex == null) {
        log.warn("Main index not found");
        return;
      }

      log.info("Main index contains:");
      log.info("  Regular packages: {}", mainIndex.getPackages().size());
      log.info("  Key packages: {}", mainIndex.getKeyPackages().size());
      log.info("  Temp packages: {}", mainIndex.getTempPackages().size());

      // Test getting all archive package file paths
      log.info("Testing getArchivePackageFilePaths()...");
      List<String> allArchiveFiles = reader.getArchivePackageFilePaths();
      log.info("Found {} total archive package files", allArchiveFiles.size());

      // Show first few files as examples
      int count = 0;
      for (String filePath : allArchiveFiles) {
        if (count++ < 10) {
          log.info("  Archive file: {}", filePath);
        }
      }
      if (allArchiveFiles.size() > 10) {
        log.info("  ... and {} more files", allArchiveFiles.size() - 10);
      }

      // Test specific package ID resolution
      log.info("Testing specific package ID resolution...");

      // Test with some known package IDs from the global index
      List<Integer> testPackageIds =
          mainIndex.getPackages().subList(0, Math.min(5, mainIndex.getPackages().size()));

      for (Integer packageId : testPackageIds) {
        log.info("Testing package ID: {}", packageId);

        // Test single main package file
        String mainPackageFile = reader.getArchivePackageFilePath(packageId);
        if (mainPackageFile != null) {
          log.info("  Main package file: {}", mainPackageFile);
        } else {
          log.info("  Main package file: NOT FOUND");
        }

        // Test all files for this package ID (including shard-specific)
        List<String> allPackageFiles = reader.getAllArchivePackageFilePaths(packageId);
        log.info("  All files for package {}: {} files", packageId, allPackageFiles.size());
        for (String file : allPackageFiles) {
          log.info("    {}", file);
        }
      }

      // Test with temp packages if available
      if (!mainIndex.getTempPackages().isEmpty()) {
        log.info("Testing temp package resolution...");
        Integer tempPackageId = mainIndex.getTempPackages().get(0);
        log.info("Testing temp package ID: {}", tempPackageId);

        String tempPackageFile = reader.getArchivePackageFilePath(tempPackageId);
        if (tempPackageFile != null) {
          log.info("  Temp package file: {}", tempPackageFile);
        } else {
          log.info("  Temp package file: NOT FOUND");
        }
      }

      log.info("✓ Archive package path resolution test completed");
    }
  }

  @Test
  public void testPackageIdToPathMapping() throws IOException {
    log.info("=== Testing Package ID to Path Mapping ===");

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {

      var mainIndex = reader.getMainIndexIndexValue();
      if (mainIndex == null) {
        log.warn("Main index not found");
        return;
      }

      // Create a mapping of package IDs to their file paths
      log.info("Creating package ID to path mapping...");

      for (Integer packageId : mainIndex.getPackages()) {
        List<String> packageFiles = reader.getAllArchivePackageFilePaths(packageId);
        if (!packageFiles.isEmpty()) {
          log.info("Package ID {} -> {} files:", packageId, packageFiles.size());
          for (String file : packageFiles) {
            log.info("  {}", file);
          }
        } else {
          log.debug("Package ID {} -> NO FILES FOUND", packageId);
        }
      }

      log.info("✓ Package ID to path mapping test completed");
    }
  }

  @Test
  public void testArchiveDirectoryStructure() throws IOException {
    log.info("=== Testing Archive Directory Structure Analysis ===");

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {

      // Test the directory scanning functionality
      List<String> allArchiveFiles = reader.getArchivePackageFilePaths();

      // Analyze the structure
      log.info("Archive directory structure analysis:");
      log.info("Total archive files found: {}", allArchiveFiles.size());

      // Group by directory
      java.util.Map<String, Integer> directoryCounts = new java.util.HashMap<>();
      java.util.Map<String, Integer> fileTypeCounts = new java.util.HashMap<>();

      for (String filePath : allArchiveFiles) {
        // Extract directory
        String directory = filePath.substring(0, filePath.lastIndexOf('/'));
        directoryCounts.merge(directory, 1, Integer::sum);

        // Extract file type (main vs shard-specific)
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        if (fileName.matches("archive\\.\\d{5}\\.pack")) {
          fileTypeCounts.merge("main", 1, Integer::sum);
        } else if (fileName.matches("archive\\.\\d{5}\\..*\\.pack")) {
          fileTypeCounts.merge("shard-specific", 1, Integer::sum);
        } else {
          fileTypeCounts.merge("other", 1, Integer::sum);
        }
      }

      log.info("Files by directory:");
      for (java.util.Map.Entry<String, Integer> entry : directoryCounts.entrySet()) {
        log.info("  {}: {} files", entry.getKey(), entry.getValue());
      }

      log.info("Files by type:");
      for (java.util.Map.Entry<String, Integer> entry : fileTypeCounts.entrySet()) {
        log.info("  {}: {} files", entry.getKey(), entry.getValue());
      }

      log.info("✓ Archive directory structure analysis completed");
    }
  }
}
