package org.ton.ton4j.tl.types.db;

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
public class TestDbReader {

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
  public void testReadArchiveDb() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();

    // Get all available archive keys
    List<String> archiveKeys = archiveDbReader.getArchiveKeys();
    log.info("Available archive keys: {}", archiveKeys);

    List<Block> blocks1 = archiveDbReader.getAllBlocks();
    log.info("All blocks: {}", blocks1.size());
    blocks1.sort(
        (o1, o2) -> {
          return Long.compare(o2.getBlockInfo().getSeqno(), o1.getBlockInfo().getSeqno());
        });
    for (Block block : blocks1) {
      //      List<ShardDescr> shardDescrs =
      // block.getExtra().getMcBlockExtra().getShardHashes().getShardDescriptionsAsList();
      //      log.info("Shard shardDescrs: {}", shardDescrs.size());
      List<InMsg> inMsgs = block.getExtra().getInMsgDesc().getInMessages();
      List<OutMsg> outMsgs = block.getExtra().getOutMsgDesc().getOutMessages();
      log.info("InMsgs: {}, OutMsgs: {}", inMsgs.size(), outMsgs.size());
      log.info(
          "({},{},{}), {} {}",
          block.getBlockInfo().getShard().getWorkchain(),
          block.getBlockInfo().getShard().convertShardIdentToShard().toString(16),
          block.getBlockInfo().getSeqno(),
          block.getBlockInfo().getPrevRef().getPrev1().getRootHash(),
          block.getBlockInfo().getPrevRef().getPrev1().getFileHash());
      //      BlockPrintInfo.printAllTransactions(block);
    }
  }

  @Test
  public void testReadArchiveDbBlockByHash() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();
    byte[] blockBytes =
        archiveDbReader.readBlock(
            "23c7e98640c2f3aa002fe66e7b785b68bd7ddfe70a1f4b354dfa9f5e10261b27");

    try {
      Cell c = CellBuilder.beginCell().fromBoc(blockBytes).endCell();
      long magic = c.getBits().preReadUint(32).longValue();
      if (magic == 0x11ef55aaL) { // block
        Block block = Block.deserialize(CellSlice.beginParse(c));
        log.info("Block: {}", block);
      } else {
        log.info("not a block");
      }
    } catch (Throwable e) {
      log.error("Error parsing block {}", e.getMessage());
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

  private Cell getFirstCellWithBlock(Cell c) {

    long blockMagic = c.getBits().preReadUint(32).longValue();
    if (blockMagic == 0x11ef55aa) {
      return c;
    }

    int i = 0;
    for (Cell ref : c.getRefs()) {
      return getFirstCellWithBlock(ref);
    }

    return null;
  }

  /** Test reading other RocksDB databases. */
  @Test
  public void testReadCellDb() throws IOException {
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
  public void testReadFilesDb() throws IOException {
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
  public void testReadAdnlDb() throws IOException {
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
