package org.ton.ton4j.tl.types.db;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.utils.Utils;

/**
 * Test class to analyze the actual format of the globalindex database entries
 * and determine the correct TL constructor IDs and data structures.
 */
@Slf4j
public class TestGlobalIndexAnalysis {

  private static final String DB_PATH = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void analyzeGlobalIndexFormat() throws IOException {
    log.info("=== Analyzing GlobalIndex Format ===");

    try (FilesDbReader reader = new FilesDbReader(DB_PATH)) {
      reader.getGlobalIndexDb().forEach((key, value) -> {
        analyzeEntry(key, value);
      });
    }
  }

  private void analyzeEntry(byte[] key, byte[] value) {
    log.info("=== Entry Analysis ===");
    log.info("Key length: {} bytes", key.length);
    log.info("Key hex: {}", Utils.bytesToHex(key));
    log.info("Value length: {} bytes", value.length);
    log.info("Value hex: {}", Utils.bytesToHex(value));

    // Analyze key
    if (key.length == 4) {
      // Likely a TL constructor for main index
      ByteBuffer keyBuffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
      int constructor = keyBuffer.getInt();
      log.info("Key as TL constructor: 0x{}", Integer.toHexString(constructor));
      
      // Check if this matches known constructors
      if (constructor == 0x7dc40502) { // Reverse byte order of 0205c47d
        log.info("This appears to be db.files.index.key (empty key)");
        analyzeIndexValue(value);
      }
    } else if (key.length == 16) {
      // Likely a TL-serialized structure
      ByteBuffer keyBuffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
      int constructor = keyBuffer.getInt();
      log.info("Key constructor: 0x{}", Integer.toHexString(constructor));
      
      if (constructor == 0xa504033e) { // Reverse byte order of 3e0304a5
        log.info("This appears to be db.files.package.key");
        analyzePackageKey(key, value);
      }
    } else {
      log.info("Unknown key format - length: {}", key.length);
    }
  }

  private void analyzeIndexValue(byte[] value) {
    log.info("Analyzing index value...");
    try {
      ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
      
      // Try to parse as db.files.index.value
      int constructor = buffer.getInt();
      log.info("Value constructor: 0x{}", Integer.toHexString(constructor));
      
      // Read vector of packages
      int packagesCount = buffer.getInt();
      log.info("Packages count: {}", packagesCount);
      
      if (packagesCount > 0 && packagesCount < 1000) { // Reasonable limit
        log.info("Package IDs:");
        for (int i = 0; i < Math.min(packagesCount, 10); i++) {
          if (buffer.remaining() >= 4) {
            int packageId = buffer.getInt();
            log.info("  Package {}: {}", i, packageId);
          }
        }
      }
    } catch (Exception e) {
      log.warn("Error parsing index value: {}", e.getMessage());
    }
  }

  private void analyzePackageKey(byte[] key, byte[] value) {
    log.info("Analyzing package key and value...");
    try {
      ByteBuffer keyBuffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
      
      // Skip constructor (already read)
      keyBuffer.getInt();
      
      // Read package_id, key, temp
      int packageId = keyBuffer.getInt();
      int keyFlag = keyBuffer.getInt(); // Bool as int
      int tempFlag = keyBuffer.getInt(); // Bool as int
      
      log.info("Package key: id={}, key={}, temp={}", packageId, keyFlag != 0, tempFlag != 0);
      
      // Analyze value
      ByteBuffer valueBuffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
      int valueConstructor = valueBuffer.getInt();
      log.info("Value constructor: 0x{}", Integer.toHexString(valueConstructor));
      
      // Try to parse package value
      int valuePackageId = valueBuffer.getInt();
      int valueKeyFlag = valueBuffer.getInt();
      int valueTempFlag = valueBuffer.getInt();
      
      log.info("Package value: id={}, key={}, temp={}", valuePackageId, valueKeyFlag != 0, valueTempFlag != 0);
      
      // Read firstblocks vector
      int firstblocksCount = valueBuffer.getInt();
      log.info("Firstblocks count: {}", firstblocksCount);
      
      for (int i = 0; i < Math.min(firstblocksCount, 5); i++) {
        if (valueBuffer.remaining() >= 24) { // workchain(4) + shard(8) + seqno(4) + unixtime(4) + lt(8)
          int workchain = valueBuffer.getInt();
          long shard = valueBuffer.getLong();
          int seqno = valueBuffer.getInt();
          int unixtime = valueBuffer.getInt();
          long lt = valueBuffer.getLong();
          
          log.info("  Firstblock {}: workchain={}, shard=0x{}, seqno={}, unixtime={}, lt={}", 
                   i, workchain, Long.toHexString(shard), seqno, unixtime, lt);
        }
      }
      
      // Read deleted flag
      if (valueBuffer.remaining() >= 4) {
        int deleted = valueBuffer.getInt();
        log.info("Deleted: {}", deleted != 0);
      }
      
    } catch (Exception e) {
      log.warn("Error parsing package key/value: {}", e.getMessage());
    }
  }
}
