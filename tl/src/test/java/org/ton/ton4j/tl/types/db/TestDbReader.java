package org.ton.ton4j.tl.types.db;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.tl.types.db.block.BlockInfo;

/** Test class for demonstrating how to use the DbReader to read TON RocksDB files. */
@Slf4j
@RunWith(JUnit4.class)
public class TestDbReader {

  private static final String TON_DB_PATH =
      "H:\\G\\Git_Projects\\MyLocalTon\\myLocalTon\\genesis\\db";
  private DbReader dbReader;

  /** Main method to run the tests. */
  public static void main(String[] args) {
    try {
      TestDbReader test = new TestDbReader();
      test.setUp();
      test.testReadArchiveDb();
      test.testReadBlockInfos();
      test.testReadPackageFiles();
      test.testReadOtherDbs();
      test.tearDown();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** Set up the test environment. */
  @Before
  public void setUp() throws IOException {
    dbReader = new DbReader(TON_DB_PATH);
  }

  /** Clean up after tests. */
  @After
  public void tearDown() throws IOException {
    if (dbReader != null) {
      dbReader.close();
    }
  }

  /** Test reading archive database. */
  @Test
  public void testReadArchiveDb() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();

    // Get all available archive keys
    List<String> archiveKeys = archiveDbReader.getArchiveKeys();
    log.info("Available archive keys: {}", archiveKeys);

    // Get all blocks
    Map<String, byte[]> blocks = archiveDbReader.getAllBlocks();
    log.info("Total blocks: {}", blocks.size());

    // Print first few blocks
    int count = 0;
    for (Map.Entry<String, byte[]> entry : blocks.entrySet()) {
      if (count++ < 5) {
        log.info(
            "Block key {}, size: {} bytes, deserialized {}",
            entry.getKey(),
            entry.getValue().length,
            ArchiveDbReader.getBlockInfo(entry.getValue()));
      }
    }
  }

  /** Test reading block infos. */
  @Test
  public void testReadBlockInfos() throws IOException {
    // Get all block infos
    Map<String, BlockInfo> blockInfos = dbReader.getAllBlockInfos();
    log.info("Total block infos: {}", blockInfos.size());

    // Print first few block infos
    int count = 0;
    for (Map.Entry<String, BlockInfo> entry : blockInfos.entrySet()) {
      if (count++ < 5) {
        BlockInfo blockInfo = entry.getValue();
        log.info("Block hash: {}", entry.getKey());
        log.info("  Version: {}", blockInfo.getVersion());
        log.info("  Gen time: {}", blockInfo.getGenUtime());
        log.info("  Start LT: {}", blockInfo.getStartLt());
        log.info("  End LT: {}", blockInfo.getEndLt());
        log.info("  Is key block: {}", blockInfo.isKeyBlock());
        log.info("  Master refs: {}", blockInfo.getMasterRefSeqno().size());
        log.info("");
      }
    }
  }

  /** Test reading package files. */
  @Test
  public void testReadPackageFiles() throws IOException {
    // This test demonstrates how to read a specific package file
    try (PackageReader packageReader =
        new PackageReader(TON_DB_PATH + "\\archive\\packages\\arch0000\\archive.00000.pack")) {
      // Read all entries in the package
      packageReader.forEach(
          entry -> {
            log.info(
                "Entry filename: {}, size: {} bytes", entry.getFilename(), entry.getData().length);
          });
    }
  }

  /** Test reading other RocksDB databases. */
  @Test
  public void testReadOtherDbs() throws IOException {
    try {
      // Open the celldb database
      RocksDbWrapper cellDb = dbReader.openDb("celldb");

      // Print some stats
      log.info("CellDB stats:");
      log.info("{}", cellDb.getStats());
    } catch (IOException e) {
      log.warn("Could not open celldb database: {}", e.getMessage());
    }

    try {
      // Open the files database
      RocksDbWrapper filesDb = dbReader.openDb("files");

      // Print some stats
      log.info("FilesDB stats:");
      log.info("{}", filesDb.getStats());
    } catch (IOException e) {
      log.warn("Could not open files database: {}", e.getMessage());
    }

    try {
      // Open the adnl database
      RocksDbWrapper adnlDb = dbReader.openDb("adnl");

      // Print some stats
      log.info("ADNLDB stats:");
      log.info("{}", adnlDb.getStats());
    } catch (IOException e) {
      log.warn("Could not open adnl database: {}", e.getMessage());
    }
  }
}
