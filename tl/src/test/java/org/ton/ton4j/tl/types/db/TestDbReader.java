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
  public void testReadAllBlocksFromArchiveDb() throws IOException {
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
      //      List<InMsg> inMsgs = block.getExtra().getInMsgDesc().getInMessages();
      //      List<OutMsg> outMsgs = block.getExtra().getOutMsgDesc().getOutMessages();
      //      log.info("InMsgs: {}, OutMsgs: {}", inMsgs.size(), outMsgs.size());
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

  /** Test reading archive database. */
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

      List<InMsg> inMsgs = block.getExtra().getInMsgDesc().getInMessages();
      List<OutMsg> outMsgs = block.getExtra().getOutMsgDesc().getOutMessages();
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

  /**
   * find block by file hash
   *
   * @throws IOException
   */
  @Test
  public void testReadArchiveDbBlockByHash() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();
    byte[] blockBytes =
        archiveDbReader.readBlock(
            "0955701B922FCB21EAEFE0BE397DF3BF56080E80FB8494A577FB5368DBF43506");

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

  /**
   * (-1,8000000000000000,701), E298281AEF17D50504F8E56BDBA0F9CF6E6EBDE04CAD82AB5CC7EFC799E8CEDB
   * 54625D5BE4BF5A00CD6419AEDA83C8F1137A4E4BEA1DB94FD550DF24F60EE420
   *
   * @throws IOException
   */
  @Test
  public void testReadArchiveDbBlockByHash2() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();
    byte[] blockBytes =
        archiveDbReader.readBlock(
            "E298281AEF17D50504F8E56BDBA0F9CF6E6EBDE04CAD82AB5CC7EFC799E8CEDB");

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

  /**
   * (0,8000000000000000,870), A809DE212158D0EF3CA99507E0041AE03DDAEF9D54E926911419A47BF36D455C
   * 860F47269086E21910F9090BA32ACEF02909886E5ECF950A18A3913673F2C197
   *
   * @throws IOException
   */
  @Test
  public void testReadArchiveDbBlockByHash3() throws IOException {
    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();
    byte[] blockBytes =
        archiveDbReader.readBlock(
            "A809DE212158D0EF3CA99507E0041AE03DDAEF9D54E926911419A47BF36D455C");

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

  private Cell getFirstCellWithBlock(Cell c) {
    // Check if this cell itself is a block
    if (c.getBits().getUsedBits() >= 32) {
      long blockMagic = c.getBits().preReadUint(32).longValue();
      if (blockMagic == 0x11ef55aa) {
        return c;
      }
    }

    // Recursively search in all references
    for (Cell ref : c.getRefs()) {
      Cell result = getFirstCellWithBlock(ref);
      if (result != null) {
        return result;
      }
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
