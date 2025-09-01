package org.ton.ton4j.tl.types.db;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.utils.Utils;

/**
 * Test class specifically for debugging and fixing the Files database global index parsing. This
 * test focuses on understanding the correct format of the globalindex RocksDB database in the Files
 * database (not Archive database).
 */
@Slf4j
public class TestFilesDbGlobalIndex {

  private static final String DB_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/archive";

  /** Test to debug the global index parsing and understand the correct format. */
  @Test
  public void testGlobalIndexDebugging() throws IOException {
    log.info("=== Debugging Files Database Global Index ===");
    log.info(
        "Based on analysis: globalindex should contain hash → (package_id, offset, size) mappings");

    try (ArchiveDbReader reader = new ArchiveDbReader(DB_PATH)) {

      // Get direct access to the global index RocksDB
      RocksDbWrapper globalIndexDb = reader.getGlobalIndexDb();

      log.info("=== Raw Global Index Analysis ===");

      AtomicInteger totalEntries = new AtomicInteger(0);
      AtomicInteger hexKeyEntries = new AtomicInteger(0);
      AtomicInteger validLocationEntries = new AtomicInteger(0);

      globalIndexDb.forEach(
          (key, value) -> {
            totalEntries.incrementAndGet();

            try {
              String keyStr = new String(key);

              // Log first few entries for analysis
              if (totalEntries.get() <= 10) {
                log.info(
                    "Entry {}: key='{}' value length={}", totalEntries.get(), keyStr, value.length);
                log.info("  Key bytes: {}, length {}", Utils.bytesToHex(key), key.length);
                log.info("  Value bytes: {} {}", Utils.bytesToHex(value), new String(value));
              }

              // Check if key looks like a hex hash
              if (isValidHexString(keyStr) && keyStr.length() == 64) {
                hexKeyEntries.incrementAndGet();

                // Try to parse the value as Files database format
                BlockLocation location = parseFilesDbValue(keyStr, value);
                if (location != null && location.isValid()) {
                  validLocationEntries.incrementAndGet();

                  if (validLocationEntries.get() <= 5) {
                    log.info(
                        "Valid BlockLocation {}: hash={}, package_id={}, offset={}, size={}",
                        validLocationEntries.get(),
                        keyStr,
                        location.getPackageId(),
                        location.getOffset(),
                        location.getSize());
                  }
                } else {
                  // Try alternative parsing methods
                  if (hexKeyEntries.get() <= 5) {
                    log.info(
                        "Failed to parse value for hash {}, trying alternative formats...", keyStr);
                    tryAlternativeParsing(keyStr, value);
                  }
                }
              }

            } catch (Exception e) {
              log.debug("Error processing entry {}: {}", totalEntries.get(), e.getMessage());
            }
          });

      log.info("=== Global Index Analysis Results ===");
      log.info("Total entries: {}", totalEntries.get());
      log.info("Hex key entries (64 chars): {}", hexKeyEntries.get());
      log.info("Valid BlockLocation entries: {}", validLocationEntries.get());
      log.info(
          "Success rate: {}%",
          validLocationEntries.get() * 100.0 / Math.max(1, hexKeyEntries.get()));

      if (validLocationEntries.get() == 0) {
        log.error("❌ No valid BlockLocations found - need to fix parsing logic");
        analyzeValueFormats(globalIndexDb);
      } else {
        log.info("✅ Found {} valid BlockLocations", validLocationEntries.get());
      }
    }
  }

  /** Parse Files database value format: package_id (8 bytes) + offset (8 bytes) + size (4 bytes) */
  private BlockLocation parseFilesDbValue(String hash, byte[] value) {
    if (value == null || value.length < 16) {
      return null; // Need at least package_id + offset
    }

    try {
      ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

      // Extract package_id (8 bytes)
      long packageId = buffer.getLong();

      // Extract offset (8 bytes)
      long offset = buffer.getLong();

      // Extract size (4 bytes if available, otherwise default)
      long size = 1024; // Default size
      if (value.length >= 20) {
        size = buffer.getInt() & 0xFFFFFFFFL; // Unsigned int
      } else if (value.length >= 24) {
        size = buffer.getLong(); // 8-byte size
      }

      // Validate values
      if (packageId < 0 || offset < 0 || size <= 0 || size > 100_000_000) {
        return null;
      }

      return BlockLocation.create(hash, packageId, offset, size);

    } catch (Exception e) {
      log.debug("Error parsing Files DB value for hash {}: {}", hash, e.getMessage());
      return null;
    }
  }

  /** Try alternative parsing methods for debugging */
  private void tryAlternativeParsing(String hash, byte[] value) {
    log.info("  Alternative parsing for hash: {}", hash);
    log.info("  Value length: {} bytes", value.length);

    if (value.length >= 8) {
      ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

      // Try as two 8-byte values
      if (value.length >= 16) {
        long val1 = buffer.getLong();
        long val2 = buffer.getLong();
        log.info("  As two longs (LE): {} and {}", val1, val2);

        // Try big-endian
        buffer = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN);
        long val1_be = buffer.getLong();
        long val2_be = buffer.getLong();
        log.info("  As two longs (BE): {} and {}", val1_be, val2_be);
      }

      // Try as multiple 4-byte values
      buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
      log.info("  As ints (LE): ");
      for (int i = 0; i < Math.min(value.length / 4, 6); i++) {
        if (buffer.remaining() >= 4) {
          int val = buffer.getInt();
          log.info("    int[{}] = {}", i, val);
        }
      }
    }

    // Try as string
    try {
      String valueStr = new String(value);
      if (valueStr.matches("^[\\x20-\\x7E]*$")) { // Printable ASCII
        log.info("  As string: '{}'", valueStr);
      }
    } catch (Exception e) {
      // Not a valid string
    }
  }

  /** Analyze different value formats in the database */
  private void analyzeValueFormats(RocksDbWrapper globalIndexDb) {
    log.info("=== Analyzing Value Formats ===");

    Map<Integer, Integer> lengthCounts = new java.util.HashMap<>();
    AtomicInteger analyzed = new AtomicInteger(0);

    globalIndexDb.forEach(
        (key, value) -> {
          if (analyzed.incrementAndGet() > 100) return; // Analyze first 100 entries

          String keyStr = new String(key);
          if (isValidHexString(keyStr) && keyStr.length() == 64) {
            lengthCounts.merge(value.length, 1, Integer::sum);
          }
        });

    log.info("Value length distribution:");
    lengthCounts.entrySet().stream()
        .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
        .forEach(entry -> log.info("  {} bytes: {} entries", entry.getKey(), entry.getValue()));
  }

  /** Get access to the global index database (reflection or package-private access) */
  private RocksDbWrapper getGlobalIndexDb(ArchiveDbReader reader) {
    try {
      // Use reflection to access the private globalIndexDb field
      java.lang.reflect.Field field = ArchiveDbReader.class.getDeclaredField("globalIndexDb");
      field.setAccessible(true);
      return (RocksDbWrapper) field.get(reader);
    } catch (Exception e) {
      log.error("Could not access globalIndexDb field: {}", e.getMessage());
      return null;
    }
  }

  /** Check if a string is a valid hexadecimal string. */
  private static boolean isValidHexString(String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }
    return s.matches("^[0-9A-Fa-f]+$");
  }

  /** Convert byte array to hex string for debugging */
  private static String bytesToHex(byte[] bytes) {
    if (bytes == null || bytes.length == 0) return "";

    StringBuilder hex = new StringBuilder();
    for (int i = 0; i < Math.min(bytes.length, 32); i++) { // Limit to first 32 bytes
      hex.append(String.format("%02x", bytes[i] & 0xFF));
    }
    if (bytes.length > 32) {
      hex.append("...");
    }
    return hex.toString();
  }
}
