package org.ton.ton4j.exporter.reader;

import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.exporter.types.BlockId;
import org.ton.ton4j.tlb.Block;

@Slf4j
public class TestTempPackageIndexReader {

  public static final int PACKAGE_TIMESTAMP = 1757055600;
  public static final String TON_DB_ROOT_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testTempPackageIndexReaderCreation() throws IOException {

    try (TempPackageIndexReader indexReader =
        new TempPackageIndexReader(TON_DB_ROOT_PATH, PACKAGE_TIMESTAMP)) {
      Map<String, Long> mappings = indexReader.getAllHashOffsetMappings();
      indexReader.close();

      try (PackageReader packageReader =
          new PackageReader(
              "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/files/packages/temp.archive."
                  + PACKAGE_TIMESTAMP
                  + ".pack")) {

        for (Map.Entry<String, Long> kv : mappings.entrySet()) {
          log.info("{} {}", kv.getKey(), kv.getValue());
          PackageReader.PackageEntry packageEntry = packageReader.getEntryAt(kv.getValue());
          log.info(" - filename {}", packageEntry.getFilename());
          //      log.info(" - seqno {}", packageEntry.getBlock().getBlockInfo().getSeqno());
        }
        log.info("total entries {}", mappings.size());
      }
    }
  }

  @Test
  public void testTempPackageIndexReaderSorted() throws IOException {

    try (TempPackageIndexReader indexReader =
        new TempPackageIndexReader(TON_DB_ROOT_PATH, PACKAGE_TIMESTAMP)) {
      Map<Long, Long> mappings = indexReader.getAllSortedOffsets();
      indexReader.close();

      try (PackageReader packageReader =
          new PackageReader(
              "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/files/packages/temp.archive."
                  + PACKAGE_TIMESTAMP
                  + ".pack")) {

        for (Map.Entry<Long, Long> kv : mappings.entrySet()) {
          log.info("{} {}", kv.getKey(), kv.getValue());
          PackageReader.PackageEntry packageEntry = packageReader.getEntryAt(kv.getKey());
          log.info("result {}", packageEntry.getFilename());
          if (packageEntry.getFilename().startsWith("block_")) {
            log.info("block {}", packageEntry.getBlock());
          }
        }
        log.info("total entries {}", mappings.size());
      }
    }
  }

  @Test
  public void testTempPackageAllBlocks() throws IOException {

    try (TempPackageIndexReader indexReader =
        new TempPackageIndexReader(TON_DB_ROOT_PATH, PACKAGE_TIMESTAMP)) {
      Map<BlockId, Block> mappings = indexReader.getAllBlocks();
      indexReader.close();

      for (Map.Entry<BlockId, Block> kv : mappings.entrySet()) {
        log.info("{} {}", kv.getKey(), kv.getValue());
      }
      log.info("total entries {}", mappings.size());
    }
  }

  @Test
  public void testTempPackageLastBlocks() throws IOException {

    TempPackageIndexReader indexReader =
        new TempPackageIndexReader(TON_DB_ROOT_PATH, PACKAGE_TIMESTAMP);
    Map<BlockId, Block> mappings = indexReader.getLast(10);
    indexReader.close();

    for (Map.Entry<BlockId, Block> kv : mappings.entrySet()) {
      log.info("{} {}", kv.getKey(), kv.getValue());
    }
  }

  @Test
  public void testTempPackageLast() throws IOException {

    TempPackageIndexReader indexReader =
        new TempPackageIndexReader(TON_DB_ROOT_PATH, PACKAGE_TIMESTAMP);
    Block block = indexReader.getLast();
    indexReader.close();
    log.info("{} {}", block.getBlockInfo().getSeqno(), block);
  }
}
