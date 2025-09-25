package org.ton.ton4j.exporter.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.exporter.types.ArchiveFileLocation;
import org.ton.ton4j.tl.types.db.files.GlobalIndexKey;
import org.ton.ton4j.tl.types.db.files.GlobalIndexValue;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.utils.Utils;

/**
 * Test class for the FilesDbReader that demonstrates the optimized block reading approach using the
 * Files database global index.
 */
@Slf4j
public class TestGlobalIndexDbReader {

  private static final String DB_PATH = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testGlobalIndexReading() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {

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
  public void testGlobalIndexReadingIndex() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {
      for (String path :
          reader.getArchivePackagesFromMainIndex()) { // still missing pack files, need to look for
        // info.x entries etc
        log.info("path: {}", path);
      }
    }
  }

  @Test
  public void testGlobalIndexReadingDirectories() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {
      for (String path :
          reader.getAllArchivePackageByDirScan()) { // this is the fastest way to get all pack files
        log.info("path: {}", path);
      }
    }
  }

  @Test
  public void testGlobalIndexReadingPathsIndexed() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {
      log.info("total indexes {}", reader.getMainIndexIndexValue().getPackages().size());
      int count = 0;
      for (Map.Entry<String, ArchiveFileLocation> path :
          reader.getAllPackagesHashOffsetMappings().entrySet()) {
        log.info("key {}, value: {}", path.getKey(), path.getValue());
        if (count++ > 5) {
          break;
        }
      }
    }
  }

  @Test
  public void testGlobalIndexReadingByPkgId() throws IOException {
    int packageId = 6400;
    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {
      Map<String, ArchiveFileLocation> result = reader.getPackageHashOffsetMappings(packageId);
      for (Map.Entry<String, ArchiveFileLocation> entry : result.entrySet()) {
        log.info("key {}, value: {}", entry.getKey(), entry.getValue());
        //        String packFilePath = entry.getValue().getIndexPath().replace("index", "pack");
        //        log.info("packFilePath: {}", packFilePath);
        //        packFilePath =
        //            packFilePath.replace("archive.06400.pack",
        // "archive.06400.0:8000000000000000.pack");
        //        PackageReader packageReader = new PackageReader(packFilePath);
        //        PackageReader.PackageEntry packageEntry =
        //            (PackageReader.PackageEntry)
        // packageReader.getEntryAt(entry.getValue().getOffset());
        //        log.info(
        //            "packageEntry: {}, dataSize {}, {}",
        //            packageEntry.getFilename(),
        //            packageEntry.getData().length,
        //            Utils.bytesToHex(packageEntry.getData()));
      }
    }
  }

  /** lists all proof_(-1,8000000000000000,125647):D35.... */
  @Test
  public void testGlobalIndexReadingIndexedPackages() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(DB_PATH)) {

      int count = 0;
      for (Map.Entry<String, ArchiveFileLocation> path :
          reader.getAllPackagesHashOffsetMappings().entrySet()) {

        PackageReader packageReader =
            new PackageReader(path.getValue().getIndexPath().replace("index", "pack"));
        PackageReader.PackageEntry packageEntry =
            (PackageReader.PackageEntry) packageReader.getEntryAt(path.getValue().getOffset());
        log.info(
            "packageEntry: {}, dataSize {}, {}",
            packageEntry.getFilename(),
            packageEntry.getData().length,
            Utils.bytesToHex(packageEntry.getData()));
        if (packageEntry.getFilename().startsWith("proof_")) { // "block_"
          log.info("Found block {}", packageEntry.getFilename());
          Block block = packageEntry.getBlock();
          log.info("block: {}", block);
        }

        packageReader.close();
        if (count++ > 5000) {
          break;
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
      List<String> filesFromIndex = reader.getArchivePackagesFromMainIndex();
      for (String path : filesFromIndex) {
        log.info("  From index: {}", path);
      }
      log.info("Total files from global index: {}", filesFromIndex.size());

      log.info("");

      // Test the new method (finds ALL files from filesystem)
      log.info("Files from filesystem scan method:");
      List<String> filesFromFilesystem = reader.getAllArchivePackageByDirScan();
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
      List<String> filesFromGlobalIndex = reader.getArchivePackagesFromMainIndex();
      log.info("   Found {} package files", filesFromGlobalIndex.size());

      log.info("");

      // Method 2: Files from filesystem scan (previous improvement)
      log.info("2. Files from filesystem scan:");
      List<String> filesFromFilesystem = reader.getAllArchivePackageByDirScan();
      log.info("   Found {} package files", filesFromFilesystem.size());

      log.info("");

      // Method 3: Files from archive index databases (NEW - C++ approach)
      log.info("3. Files from archive index databases (C++ approach):");
      Map<String, ArchiveFileLocation> fileLocations = reader.getAllPackagesHashOffsetMappings();
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
          log.info(
              "  Hash: {} -> Package: {}, Offset: {}",
              hash.substring(0, 16) + "...",
              location.getPackageId(),
              location.getOffset());
        }
      }

      log.info("");

      // Final comparison
      log.info("=== FINAL COMPARISON ===");
      log.info("Method 1 (Global Index): {} package files", filesFromGlobalIndex.size());
      log.info("Method 2 (Filesystem Scan): {} package files", filesFromFilesystem.size());
      log.info(
          "Method 3 (Archive Index DBs): {} individual files in {} packages",
          fileLocations.size(),
          packageFileCounts.size());

      log.info("");
      log.info("The C++ approach (Method 3) provides complete access to individual files");
      log.info("within archive packages using hash->offset lookups, just like the original");
      log.info("TON implementation.");
    }
  }
}
