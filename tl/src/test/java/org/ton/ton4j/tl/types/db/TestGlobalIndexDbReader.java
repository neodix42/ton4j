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
}
