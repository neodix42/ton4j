package org.ton.ton4j.exporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.exporter.types.ExportedBlock;

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
      // Initialize exporter
      Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

      // Setup database and table
      setupDatabase();

      AtomicInteger blockCount = new AtomicInteger(0);
      AtomicInteger insertedCount = new AtomicInteger(0);

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

                    // Create JSON data containing block information
                    BlockData blockData =
                        BlockData.builder()
                            .workchain(workchain)
                            .shard(shardHex)
                            .seqno(seqno)
                            .rawData(block.getRawData())
                            .archiveKey(block.getArchiveKey())
                            .blockKey(block.getBlockKey())
                            .build();

                    String jsonData = gson.toJson(blockData);

                    // Insert into database
                    preparedStatement.setInt(1, workchain);
                    preparedStatement.setString(2, shardHex);
                    preparedStatement.setLong(3, seqno);
                    preparedStatement.setString(4, jsonData);
                    preparedStatement.executeUpdate();

                    insertedCount.incrementAndGet();

                    log.info(
                        "Inserted block - Workchain: {}, Shard: {}, Seqno: {}",
                        workchain,
                        shardHex,
                        seqno);

                  } catch (SQLException e) {
                    log.error("Failed to insert block: {}", e.getMessage());
                  } catch (NumberFormatException e) {
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

      log.info(
          "Total blocks processed: {}, Successfully inserted: {}",
          blockCount.get(),
          insertedCount.get());

    } catch (Exception e) {
      log.warn("Test skipped - database or TON database path not available: {}", e.getMessage());
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
