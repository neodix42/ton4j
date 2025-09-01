package org.ton.ton4j.tl.types.db;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * Test class to verify the new archive package file discovery using global index. This test
 * demonstrates the replacement of old file discovery with global index-based approach.
 */
@Slf4j
public class TestArchivePackageDiscovery {

  private static final String DB_PATH = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testArchivePackageFileDiscovery() throws IOException {
    log.info("=== Testing Archive Package File Discovery from Global Index ===");

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {

      // Test the new global index-based method
      log.info("Getting archive package file paths from global index...");
      List<String> archivePackageFiles = reader.getArchivePackageFilePaths();

      log.info("Found {} archive package files:", archivePackageFiles.size());
      for (int i = 0; i < Math.min(archivePackageFiles.size(), 10); i++) {
        log.info("  Archive file {}: {}", i + 1, archivePackageFiles.get(i));
      }

      if (archivePackageFiles.size() > 10) {
        log.info("  ... and {} more files", archivePackageFiles.size() - 10);
      }

      // Verify the main index was loaded
      var mainIndex = reader.getMainIndexIndexValue();
      if (mainIndex != null) {
        log.info("Main index loaded successfully:");
        log.info("  Regular packages: {}", mainIndex.getPackages().size());
        log.info("  Key packages: {}", mainIndex.getKeyPackages().size());
        log.info("  Temp packages: {}", mainIndex.getTempPackages().size());
        log.info(
            "  Total expected archive files: {}",
            mainIndex.getPackages().size()
                + mainIndex.getKeyPackages().size()
                + mainIndex.getTempPackages().size());
      } else {
        log.warn("Main index not loaded");
      }

      // Test results
      if (archivePackageFiles.isEmpty()) {
        log.warn("⚠️  No archive package files found - this might indicate:");
        log.warn("    1. Archive files don't exist at the expected location");
        log.warn("    2. Package IDs from global index don't match archive file names");
        log.warn("    3. Archive directory path is incorrect");
      } else {
        log.info(
            "✅ Successfully discovered {} archive package files using global index",
            archivePackageFiles.size());
      }

      log.info("=== Archive Package File Discovery Test Completed ===");
    }
  }
}
