package org.ton.ton4j.exporter.reader;

import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.*;

/** Test class for demonstrating how to use the DbReader to read TON RocksDB files. */
@Slf4j
@RunWith(JUnit4.class)
public class TestArchiveDbReader {

  private static final String TON_DB_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";
  private DbReader dbReader;

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
  public void testReadAllBlocksFromArchiveDb() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();

    // Get all available archive keys
    List<String> archiveKeys = archiveDbReader.getArchiveKeys();
    log.info("Available archive files: {}", archiveKeys);

    LinkedHashSet<Block> blocks1 = archiveDbReader.getAllBlocks();
    log.info("All blocks: {}", blocks1.size());
    //    blocks1.sort(
    //        (o1, o2) -> Long.compare(o2.getBlockInfo().getSeqno(), o1.getBlockInfo().getSeqno()));
    for (Block block : blocks1) {
      log.info(
          "({},{},{}), {} {}",
          block.getBlockInfo().getShard().getWorkchain(),
          block.getBlockInfo().getShard().convertShardIdentToShard().toString(16),
          block.getBlockInfo().getSeqno(),
          block.getBlockInfo().getPrevRef().getPrev1().getRootHash().toUpperCase(),
          block.getBlockInfo().getPrevRef().getPrev1().getFileHash().toUpperCase());
      //      BlockPrintInfo.printAllTransactions(block);
    }
  }

  /** Test block rate. single thread 1000 blocks per second (no tuning) */
  @Test
  public void testReadAllBlocksFromArchiveDbBlockRate() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();

    // Get all available archive keys
    List<String> archiveKeys = archiveDbReader.getArchiveKeys();
    log.info("Available archive keys: {}", archiveKeys);

    // Measure time for getAllBlocks() method
    log.info("Starting to load all blocks...");
    long startTime = System.currentTimeMillis();

    LinkedHashSet<Block> blocks1 = archiveDbReader.getAllBlocks();

    long endTime = System.currentTimeMillis();
    long durationMs = endTime - startTime;
    double durationSeconds = durationMs / 1000.0;

    int totalBlocks = blocks1.size();
    double blocksPerSecond = totalBlocks / durationSeconds;

    log.info("All blocks loaded: {}", totalBlocks);
    log.info(
        "Loading time: {} ms ({} seconds)", durationMs, String.format("%.2f", durationSeconds));
    log.info("Loading rate: {} blocks per second", blocksPerSecond);
    log.info(
        "Average time per block: {} ms", String.format("%.2f", (double) durationMs / totalBlocks));
  }

  /** Test parallel block reading with 32 threads (default) */
  @Test
  public void testReadAllBlocksFromArchiveDbParallel() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();

    // Get all available archive keys
    List<String> archiveKeys = archiveDbReader.getArchiveKeys();
    log.info("Available archive keys: {}", archiveKeys);

    // Measure time for getAllBlocksParallel() method with default 32 threads
    log.info("Starting to load all blocks in parallel (32 threads)...");
    long startTime = System.currentTimeMillis();

    LinkedHashSet<Block> parallelBlocks = archiveDbReader.getAllBlocksParallel();

    long endTime = System.currentTimeMillis();
    long durationMs = endTime - startTime;
    double durationSeconds = durationMs / 1000.0;

    int totalBlocks = parallelBlocks.size();
    double blocksPerSecond = totalBlocks / durationSeconds;

    log.info("All blocks loaded in parallel: {}", totalBlocks);
    log.info(
        "Parallel loading time: {} ms ({} seconds)",
        durationMs,
        String.format("%.2f", durationSeconds));
    log.info("Parallel loading rate: {} blocks per second", String.format("%.2f", blocksPerSecond));
    log.info(
        "Average time per block: {} ms", String.format("%.4f", (double) durationMs / totalBlocks));
  }

  /** Test parallel block reading with custom thread count */
  @Test
  public void testReadAllBlocksFromArchiveDbParallelCustomThreads() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();

    // Get all available archive keys
    List<String> archiveKeys = archiveDbReader.getArchiveKeys();
    log.info("Available archive keys: {}", archiveKeys);

    int customThreadCount = 16;

    // Measure time for getAllBlocksParallel() method with custom thread count
    log.info("Starting to load all blocks in parallel ({} threads)...", customThreadCount);
    long startTime = System.currentTimeMillis();

    LinkedHashSet<Block> parallelBlocks = archiveDbReader.getAllBlocksParallel(customThreadCount);

    long endTime = System.currentTimeMillis();
    long durationMs = endTime - startTime;
    double durationSeconds = durationMs / 1000.0;

    int totalBlocks = parallelBlocks.size();
    double blocksPerSecond = totalBlocks / durationSeconds;

    log.info("All blocks loaded in parallel: {}", totalBlocks);
    log.info(
        "Parallel loading time: {} ms ({} seconds)",
        durationMs,
        String.format("%.2f", durationSeconds));
    log.info("Parallel loading rate: {} blocks per second", String.format("%.2f", blocksPerSecond));
    log.info(
        "Average time per block: {} ms", String.format("%.4f", (double) durationMs / totalBlocks));
  }

  /** Test parallel entries reading performance comparison */
  @Test
  public void testReadAllEntriesParallelComparison() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();

    // Get all available archive keys
    List<String> archiveKeys = archiveDbReader.getArchiveKeys();
    log.info("Available archive keys: {}", archiveKeys);

    // Test single-threaded version
    log.info("Starting single-threaded getAllEntries()...");
    long singleStartTime = System.currentTimeMillis();
    Map<String, byte[]> singleThreadedEntries = archiveDbReader.getAllEntries();
    long singleEndTime = System.currentTimeMillis();
    long singleDurationMs = singleEndTime - singleStartTime;

    log.info("Single-threaded entries loaded: {}", singleThreadedEntries.size());
    log.info("Single-threaded time: {} ms", singleDurationMs);

    // Test parallel version
    log.info("Starting parallel getAllEntriesParallel()...");
    long parallelStartTime = System.currentTimeMillis();
    Map<String, byte[]> parallelEntries = archiveDbReader.getAllEntriesParallel();
    long parallelEndTime = System.currentTimeMillis();
    long parallelDurationMs = parallelEndTime - parallelStartTime;

    log.info("Parallel entries loaded: {}", parallelEntries.size());
    log.info("Parallel time: {} ms", parallelDurationMs);

    // Compare results
    double speedup = (double) singleDurationMs / parallelDurationMs;
    log.info("Speedup: {}x", String.format("%.2f", speedup));
    log.info("Performance improvement: {}%", String.format("%.1f", (speedup - 1) * 100));

    // Verify that both methods return the same number of entries
    if (singleThreadedEntries.size() != parallelEntries.size()) {
      log.warn(
          "Entry count mismatch: single={}, parallel={}",
          singleThreadedEntries.size(),
          parallelEntries.size());
    } else {
      log.info("✓ Entry counts match: {}", singleThreadedEntries.size());
    }
  }

  @Test
  public void testReadAllBlocksAndTheirHashesFromArchiveDb() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();

    // Get all available archive keys
    List<String> archiveKeys = archiveDbReader.getArchiveKeys();
    log.info("Available archive keys: {}", archiveKeys);

    Map<String, Block> entries = archiveDbReader.getAllBlocksWithHashes();

    log.info("All entries: {}", entries.size());
    for (Map.Entry<String, Block> entry : entries.entrySet()) {
      Block block = entry.getValue();
      String hash = entry.getKey();

      //      List<InMsg> inMsgs = block.getExtra().getInMsgDesc().getInMessages();
      //      List<OutMsg> outMsgs = block.getExtra().getOutMsgDesc().getOutMessages();
      //      log.info("InMsgs: {}, OutMsgs: {}", inMsgs.size(), outMsgs.size());
      log.info(
          "hash {} ({},{},{}), {} {}",
          hash,
          block.getBlockInfo().getShard().getWorkchain(),
          block.getBlockInfo().getShard().convertShardIdentToShard().toString(16),
          block.getBlockInfo().getSeqno(),
          block.getBlockInfo().getPrevRef().getPrev1().getRootHash().toUpperCase(),
          block.getBlockInfo().getPrevRef().getPrev1().getFileHash().toUpperCase());
      //      BlockPrintInfo.printAllTransactions(block);
    }
  }

  @Test
  public void testReadAllBlocksAndTheirHashesParallel() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();

    // Get all available archive keys
    List<String> archiveKeys = archiveDbReader.getArchiveKeys();
    log.info("Available archive keys: {}", archiveKeys);

    Map<String, Block> entries = archiveDbReader.getAllBlocksWithHashesParallel();

    log.info("All entries: {}", entries.size());
    for (Map.Entry<String, Block> entry : entries.entrySet()) {
      Block block = entry.getValue();
      String hash = entry.getKey();
      log.info(
          "hash {} ({},{},{}), {} {}",
          hash,
          block.getBlockInfo().getShard().getWorkchain(),
          block.getBlockInfo().getShard().convertShardIdentToShard().toString(16),
          block.getBlockInfo().getSeqno(),
          block.getBlockInfo().getPrevRef().getPrev1().getRootHash().toUpperCase(),
          block.getBlockInfo().getPrevRef().getPrev1().getFileHash().toUpperCase());
    }
  }

  /** find block by file hash */
  @Test
  public void testReadArchiveDbBlockByHash() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();
    byte[] blockBytes =
        archiveDbReader.readBlock(
            "73DB616CB476C8887EBAFE25989E8D3735FEB7BA59470919C3CDA2BD35B749EF");

    if (blockBytes == null) {
      log.error("Block not found!");
      return;
    }

    log.info("Block found, size: {} bytes", blockBytes.length);

    try {
      Cell c = CellBuilder.beginCell().fromBoc(blockBytes).endCell();
      long magic = c.getBits().preReadUint(32).longValue();
      log.info("Magic number: 0x{}", Long.toHexString(magic));

      if (magic == 0x11ef55aaL) { // block
        Block block = Block.deserialize(CellSlice.beginParse(c));
        log.info("Successfully parsed Block: {}", block);
      } else if ((magic & 0xFF000000L) == 0xc3000000L) { // BlockProof (starts with 0xc3)
        log.info("Found BlockProof, extracting block from it...");
        BlockProof blockProof = BlockProof.deserialize(CellSlice.beginParse(c));
        log.info("Successfully parsed BlockProof: {}", blockProof);
      } else {
        log.info(
            "Unknown data type, magic is 0x{} (expected 0x11ef55aa for Block or 0xc3xxxxxx for BlockProof)",
            Long.toHexString(magic));
      }
    } catch (Throwable e) {
      log.error("Error parsing block: {}", e.getMessage());
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
            Cell c = entry.getCell();
            log.info(
                "Entry filename: {}, size: {} bytes, boc {}",
                entry.getFilename(),
                entry.getData().length,
                c);
            if (c.getBits().preReadUint(8).longValue() == 0xc3) {
              BlockProof blockProof = BlockProof.deserialize(CellSlice.beginParse(c));
              log.info("deserialized blockProof: {}", blockProof);
            } else if (c.getBits().preReadUint(32).longValue() == 0x11ef55aa) {
              //              Cell blockCell = getFirstCellWithBlock(c);
              //              Block block = Block.deserialize(CellSlice.beginParse(c));
              //              log.info("deserialized block: {}", block);
            }
          });
    }
  }

  /** Test reading other RocksDB databases. */
  @Test
  public void testReadCellDbStats() {
    try {
      // Open the celldb database
      RocksDbWrapper cellDb = dbReader.openDb("celldb");

      // Print some stats
      log.info("CellDB stats:");
      log.info("{}", cellDb.getStats());
    } catch (IOException e) {
      log.warn("Could not open celldb database: {}", e.getMessage());
    }
  }

  @Test
  public void testReadFilesDbStats() {
    try {
      // Open the files database
      RocksDbWrapper filesDb = dbReader.openDb("files/globalindex");

      // Print some stats
      log.info("FilesDB stats:");
      log.info("{}", filesDb.getStats());
    } catch (IOException e) {
      log.warn("Could not open files database: {}", e.getMessage());
    }
  }

  @Test
  public void testReadAdnlDbStats() {
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
