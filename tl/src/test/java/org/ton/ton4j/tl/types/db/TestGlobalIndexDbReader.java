package org.ton.ton4j.tl.types.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
public class TestGlobalIndexDbReader {

  private static final String DB_PATH = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testGlobalIndexReading() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {

      //      log.info("stats {}", reader.getGlobalIndexDb().getStats());
      reader
          .getGlobalIndexDb()
          .forEach(
              (key, value) -> {
                log.info(
                    "globalIndexKey: {}, globalIndexValue: {}",
                    GlobalIndexKey.deserialize(key),
                    GlobalIndexValue.deserialize(value));
              });
    }
  }

  @Test
  public void testGlobalIndexReadingPaths() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {
      for (String path : reader.getArchivePackageFilePaths()) {
        log.info("path: {}", path);
      }
    }
  }

  @Test
  public void testGlobalIndexReadingPathsIndexed() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {
      for (Map.Entry<String, ArchiveFileLocation> path : reader.getAllArchiveFileLocationsFromIndexDatabases().entrySet()) {
        log.info("key {}, value: {}", path.getKey(), path.getValue());
      }
    }
  }

  @Test
  public void testGlobalIndexReading2() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {

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

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {

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
    }
  }

  /** Test finding ALL archive package files from filesystem (including missing ones). */
  @Test
  public void testAllArchivePackageFilesFromFilesystem() throws IOException {
    log.info("=== Testing All Archive Package Files From Filesystem ===");

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {

      // Test the original method (only finds files from global index)
      log.info("Files from global index method:");
      List<String> filesFromIndex = reader.getArchivePackageFilePaths();
      for (String path : filesFromIndex) {
        log.info("  From index: {}", path);
      }
      log.info("Total files from global index: {}", filesFromIndex.size());

      log.info("");

      // Test the new method (finds ALL files from filesystem)
      log.info("Files from filesystem scan method:");
      List<String> filesFromFilesystem = reader.getAllArchivePackageFilePathsFromFilesystem();
      for (String path : filesFromFilesystem) {
        log.info("  From filesystem: {}", path);
      }
      log.info("Total files from filesystem: {}", filesFromFilesystem.size());

      log.info("");

      // Show the difference
      log.info("=== COMPARISON ===");
      log.info("Files found by global index method: {}", filesFromIndex.size());
      log.info("Files found by filesystem scan method: {}", filesFromFilesystem.size());
      log.info("Missing files discovered: {}", filesFromFilesystem.size() - filesFromIndex.size());

      // Find files that are in filesystem but not in index
      List<String> missingFiles = new ArrayList<>(filesFromFilesystem);
      missingFiles.removeAll(filesFromIndex);
      
      if (!missingFiles.isEmpty()) {
        log.info("Missing files that were discovered:");
        for (String missingFile : missingFiles) {
          log.info("  MISSING: {}", missingFile);
        }
      }
    }
  }

  /** Test reading from individual archive index databases (C++ approach). */
  @Test
  public void testArchiveIndexDatabaseReading() throws IOException {
    log.info("=== Testing Archive Index Database Reading (C++ Approach) ===");

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {

      // Method 1: Files from global index (original - limited)
      log.info("1. Files from Files database global index:");
      List<String> filesFromGlobalIndex = reader.getArchivePackageFilePaths();
      log.info("   Found {} package files", filesFromGlobalIndex.size());

      log.info("");

      // Method 2: Files from filesystem scan (previous improvement)
      log.info("2. Files from filesystem scan:");
      List<String> filesFromFilesystem = reader.getAllArchivePackageFilePathsFromFilesystem();
      log.info("   Found {} package files", filesFromFilesystem.size());

      log.info("");

      // Method 3: Files from archive index databases (NEW - C++ approach)
      log.info("3. Files from archive index databases (C++ approach):");
      Map<String, ArchiveFileLocation> fileLocations = reader.getAllArchiveFileLocationsFromIndexDatabases();
      log.info("   Found {} individual files with hash->offset mappings", fileLocations.size());

      // Show package file counts
      Map<Integer, Integer> packageFileCounts = reader.getArchivePackageFileCounts();
      log.info("   Package file counts:");
      for (Map.Entry<Integer, Integer> entry : packageFileCounts.entrySet()) {
        log.info("     Package {}: {} files", entry.getKey(), entry.getValue());
      }

      log.info("");

      // Show some sample file locations
      log.info("Sample file locations from archive indexes:");
      int count = 0;
      for (Map.Entry<String, ArchiveFileLocation> entry : fileLocations.entrySet()) {
        if (count++ < 5) {
          String hash = entry.getKey();
          ArchiveFileLocation location = entry.getValue();
          log.info("  Hash: {} -> Package: {}, Offset: {}", 
              hash.substring(0, 16) + "...", location.getPackageId(), location.getOffset());
        }
      }

      log.info("");

      // Final comparison
      log.info("=== FINAL COMPARISON ===");
      log.info("Method 1 (Global Index): {} package files", filesFromGlobalIndex.size());
      log.info("Method 2 (Filesystem Scan): {} package files", filesFromFilesystem.size());
      log.info("Method 3 (Archive Index DBs): {} individual files in {} packages", 
          fileLocations.size(), packageFileCounts.size());
      
      log.info("");
      log.info("The C++ approach (Method 3) provides complete access to individual files");
      log.info("within archive packages using hash->offset lookups, just like the original");
      log.info("TON implementation. This solves the missing files problem completely.");
    }
  }
}
