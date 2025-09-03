package org.ton.ton4j.exporter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.exporter.types.BlockId;
import org.ton.ton4j.exporter.types.ExportedBlock;
import org.ton.ton4j.tlb.Block;

@Slf4j
@RunWith(JUnit4.class)
public class TestExporter {

  public static final String TON_DB_ROOT_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testExporterBuilder() {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    assertThat(exporter).isNotNull();
    log.info("exporter root db path {}", exporter.getDatabasePath());
  }

  @Test
  public void testExporterArchiveStats() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    assertThat(exporter).isNotNull();
    log.info("exporter root db path {}", exporter.getDatabasePath());
    exporter.printADbStats();
  }

  @Test
  public void testExporterToFile() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    assertThat(exporter).isNotNull();
    FileUtils.deleteQuietly(new File("local.txt"));
    exporter.exportToFile("local.txt", true, 20);
  }

  @Test
  public void testExporterToStdout() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    exporter.exportToStdout(false, 20);
  }

  @Test
  public void testExporterToObjects() throws IOException {
    Exporter exporter =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();

    Stream<ExportedBlock> blockStream = exporter.exportToObjects(true, 20);

    blockStream.forEach(
        b -> {
          // insert block to your DB
          //              log.info("block {}", b);
        });
    blockStream.close(); // to delete status file
  }

  @Test
  public void testExporterGetLast() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

    long startTime = System.currentTimeMillis();

    Block latestBlock = exporter.getLast();

    long durationMs = System.currentTimeMillis() - startTime;

    log.info("received last block : {}ms", durationMs);

    log.info("Latest block found:");
    log.info("  Workchain: {}", latestBlock.getBlockInfo().getShard().getWorkchain());
    log.info(
        "  Shard: {}",
        latestBlock.getBlockInfo().getShard().convertShardIdentToShard().toString(16));
    log.info("  Sequence Number: {}", latestBlock.getBlockInfo().getSeqno());
    log.info("  Timestamp: {}", latestBlock.getBlockInfo().getGenuTime());
  }

  @Test
  public void testExporterGetLastWithLimit() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

    long startTime = System.currentTimeMillis();

    TreeMap<BlockId, Block> latestBlocks = exporter.getLast(10);

    long durationMs = System.currentTimeMillis() - startTime;

    log.info("received last block : {}ms", durationMs);

    latestBlocks.forEach(
        (blockId, block) -> {
          log.info("blockId {}, {}", blockId, block);
        });
  }
}
