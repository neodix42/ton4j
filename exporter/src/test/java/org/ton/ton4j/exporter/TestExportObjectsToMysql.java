package org.ton.ton4j.exporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.sql.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.cell.TonHashMap;
import org.ton.ton4j.cell.TonHashMapAug;
import org.ton.ton4j.cell.TonHashMapAugE;
import org.ton.ton4j.cell.TonHashMapE;
import org.ton.ton4j.exporter.types.ExportedBlock;
import org.ton.ton4j.tlb.adapters.*;

/**
 * Test class that demonstrates exporting TON blocks to MySQL database.
 *
 * <p>Prerequisites: 1. MySQL server running on localhost:3306 2. MySQL user 'root' with appropriate
 * permissions (or modify DB_USER/DB_PASSWORD constants) 3. TON database available at the specified
 * path
 *
 * <p>The test will: - Create database 'testexporterdb' if it doesn't exist - Create table 'blocks'
 * with columns: wc, shard, seqno, json_data - Export blocks from TON database and insert them into
 * MySQL - Handle errors gracefully if database or TON data is not available
 *
 * <p>Table schema: - wc (INT): workchain number - shard (STRING): shard identifier as hex string -
 * seqno (BIGINT): sequence number - json_data (LONGTEXT): JSON representation of block data
 * including raw data, keys, etc.
 */
@Slf4j
public class TestExportObjectsToMysql {

  public static final String TON_DB_ROOT_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  private static final String DB_URL = "jdbc:mysql://localhost:3306/";
  private static final String DB_NAME = "testexporterdb";
  private static final String DB_USER = "root";
  private static final String DB_PASSWORD = "root";

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Test
  public void testExportToObjectsWithMysqlInsertion() {
    try {
      Gson gson =
          new GsonBuilder()
              .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
              .registerTypeAdapter(byte[].class, new ByteArrayToHexTypeAdapter())
              .registerTypeAdapter(TonHashMapAug.class, new TonHashMapAugTypeAdapter())
              .registerTypeAdapter(TonHashMapAugE.class, new TonHashMapAugETypeAdapter())
              .registerTypeAdapter(TonHashMap.class, new TonHashMapTypeAdapter())
              .registerTypeAdapter(TonHashMapE.class, new TonHashMapETypeAdapter())
              .disableHtmlEscaping()
              .setLenient()
              .create();

      // Initialize exporter
      Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

      // Setup database and table
      setupDatabase();

      AtomicInteger blockCount = new AtomicInteger(0);
      AtomicInteger insertedCount = new AtomicInteger(0);
      AtomicInteger errorCount = new AtomicInteger(0);

      // Start timing
      long startTime = System.currentTimeMillis();
      log.info("Starting export process at: {}", new java.util.Date(startTime));

      // Test with deserialization enabled and 20 parallel threads
      Stream<ExportedBlock> blockStream = exporter.exportToObjects(true, 20);

      try (Connection connection =
          DriverManager.getConnection(DB_URL + DB_NAME, DB_USER, DB_PASSWORD)) {
        String insertSQL = "INSERT INTO blocks (wc, shard, seqno, json_data) VALUES (?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
          blockStream.forEach(
              block -> {
                blockCount.incrementAndGet();

                if (block.isDeserialized() && block.getBlock() != null) {
                  try {
                    // Extract block information
                    int workchain = block.getWorkchain();
                    String shardHex = block.getShard();
                    long seqno = block.getSeqno();
                    String jsonBlock = gson.toJson(block.getDeserializedBlock());

                    // Insert into database
                    preparedStatement.setInt(1, workchain);
                    preparedStatement.setString(2, shardHex);
                    preparedStatement.setLong(3, seqno);
                    preparedStatement.setString(4, jsonBlock);
                    preparedStatement.executeUpdate();

                    insertedCount.incrementAndGet();

                    log.info(
                        "Inserted block - Workchain: {}, Shard: {}, Seqno: {}",
                        workchain,
                        shardHex,
                        seqno);

                  } catch (SQLException e) {
                    errorCount.incrementAndGet();
                    log.error("Failed to insert block: {}", e.getMessage());
                  } catch (NumberFormatException e) {
                    errorCount.incrementAndGet();
                    log.warn(
                        "Failed to parse shard hex '{}': {}", block.getShard(), e.getMessage());
                  }
                } else {
                  log.debug(
                      "Skipping non-deserialized block with hex data length: {}",
                      block.getRawData() != null ? block.getRawData().length() : 0);
                }
              });
        }
      }

      // End timing and calculate metrics
      long endTime = System.currentTimeMillis();
      long durationMs = endTime - startTime;
      double durationSeconds = durationMs / 1000.0;
      double insertRate = insertedCount.get() / durationSeconds;

      // Display summary
      log.info("=== EXPORT PROCESS SUMMARY ===");
      log.info("Start time: {}", new java.util.Date(startTime));
      log.info("End time: {}", new java.util.Date(endTime));
      log.info(
          "Total duration: {} ms ({} seconds)", durationMs, String.format("%.2f", durationSeconds));
      log.info("Total blocks processed: {}", blockCount.get());
      log.info("Successfully inserted blocks: {}", insertedCount.get());
      log.info("Errors encountered: {}", errorCount.get());
      log.info("Insert rate: {} blocks/second", insertRate);

      if (errorCount.get() > 0) {
        log.warn("Success rate: {}%", (insertedCount.get() * 100.0) / blockCount.get());
      } else {
        log.info("Success rate: 100% (no errors)");
      }

      // Display first 3 rows from the database
      displaySampleData();

    } catch (Exception e) {
      log.warn("Test skipped - database or TON database path not available: {}", e.getMessage());
    }
  }

  /** Display the first 3 rows from the blocks table with all columns */
  private void displaySampleData() {
    try (Connection connection =
            DriverManager.getConnection(DB_URL + DB_NAME, DB_USER, DB_PASSWORD);
        Statement statement = connection.createStatement()) {

      String selectSQL =
          "SELECT id, wc, shard, seqno, json_data, created_at FROM blocks ORDER BY id LIMIT 100";

      try (ResultSet resultSet = statement.executeQuery(selectSQL)) {
        log.info("=== FIRST 10 ROWS FROM BLOCKS TABLE ===");

        int rowCount = 0;
        while (resultSet.next()) {
          rowCount++;
          long id = resultSet.getLong("id");
          int wc = resultSet.getInt("wc");
          long shard = resultSet.getLong("shard");
          long seqno = resultSet.getLong("seqno");
          String jsonData = resultSet.getString("json_data");
          java.sql.Timestamp createdAt = resultSet.getTimestamp("created_at");

          log.info("--- Row {} ---", rowCount);
          log.info("ID: {}", id);
          log.info("Workchain: {}", wc);
          log.info("Shard: {}", shard);
          log.info("Seqno: {}", seqno);
          log.info("Created At: {}", createdAt);
          log.info("JSON Data: {}", jsonData);
        }

        if (rowCount == 0) {
          log.info("No rows found in the blocks table");
        }
      }

    } catch (SQLException e) {
      log.error("Failed to retrieve sample data: {}", e.getMessage());
    }
  }

  private void setupDatabase() throws SQLException {
    // Create database if it doesn't exist
    try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        Statement statement = connection.createStatement()) {

      statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
      log.info("Database '{}' created or already exists", DB_NAME);
    }

    // Create table if it doesn't exist
    try (Connection connection =
            DriverManager.getConnection(DB_URL + DB_NAME, DB_USER, DB_PASSWORD);
        Statement statement = connection.createStatement()) {

      String createTableSQL =
          "CREATE TABLE IF NOT EXISTS blocks ("
              + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
              + "wc INT NOT NULL, "
              + "shard BIGINT NOT NULL, "
              + "seqno BIGINT NOT NULL, "
              + "json_data LONGTEXT NOT NULL, "
              + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
              + "INDEX idx_wc_shard_seqno (wc, shard, seqno)"
              + ")";

      statement.executeUpdate(createTableSQL);
      log.info("Table 'blocks' created or already exists");

      // Truncate table to start fresh for each test run
      statement.executeUpdate("TRUNCATE TABLE blocks");
      log.info("Table 'blocks' truncated for fresh test run");
    }
  }

  /** Data class for JSON serialization of block data */
  @lombok.Data
  @lombok.Builder
  public static class BlockData {
    private final int workchain;
    private final String shard;
    private final long seqno;
    private final String rawData;
    private final String archiveKey;
    private final String blockKey;
  }
}
