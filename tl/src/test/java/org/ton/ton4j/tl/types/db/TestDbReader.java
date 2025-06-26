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
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.tlb.BlockInfo;
import org.ton.ton4j.tlb.BlockProof;

/** Test class for demonstrating how to use the DbReader to read TON RocksDB files. */
@Slf4j
@RunWith(JUnit4.class)
public class TestDbReader {

  private static final String TON_DB_PATH =
      "H:\\G\\Git_Projects\\MyLocalTon\\myLocalTon\\genesis\\db";
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
      log.info(
          "{}:{}:{} prevRootHash {}, prefFileHash {}, {}",
          block.getBlockInfo().getShard().getWorkchain(),
          block.getBlockInfo().getSeqno(),
          block.getBlockInfo().getShard().convertShardIdentToShard().toString(16),
          block.getBlockInfo().getPrevRef().getPrev1().getRootHash(),
          block.getBlockInfo().getPrevRef().getPrev1().getFileHash(),
          block.getStateUpdate().getNewHash());
    }

    //    // Get all blocks
    //    Map<String, byte[]> blocks = archiveDbReader.getAllEntries();
    //    log.info("Total blocks: {}", blocks.size());
    //
    //    // Print first few blocks
    //    int count = 0;
    //    for (Map.Entry<String, byte[]> entry : blocks.entrySet()) {
    //      if (count++ < 385) {
    //        Block block = ArchiveDbReader.getBlock(entry.getValue());
    //        log.info("Block: {}", block);
    //        log.info(
    //            "idx {}, Block key {}, size: {} bytes, deserialized {}",
    //            count,
    //            entry.getKey(),
    //            entry.getValue().length,
    //            block.getBlockInfo());
    //      }
    //    }
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
        log.info("  Gen time: {}", blockInfo.getGenuTime());
        log.info("  Start LT: {}", blockInfo.getStartLt());
        log.info("  End LT: {}", blockInfo.getEndLt());
        log.info("  Is key block: {}", blockInfo.isKeyBlock());
        log.info("  Master refs: {}", blockInfo.getMasterRef().getSeqno());
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
