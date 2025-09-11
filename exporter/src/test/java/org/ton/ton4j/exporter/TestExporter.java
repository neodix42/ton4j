package org.ton.ton4j.exporter;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.exporter.types.BlockId;
import org.ton.ton4j.exporter.types.ByteArrayToHexTypeAdapter;
import org.ton.ton4j.exporter.types.ExportedBlock;
import org.ton.ton4j.tlb.Account;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.utils.Utils;

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
    Exporter exporter =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    assertThat(exporter).isNotNull();
    FileUtils.deleteQuietly(new File("local.txt"));
    exporter.exportToFile("local.txt", true, 20);
  }

  @Test
  public void testExporterToFileNotDeserialized() throws IOException {
    Exporter exporter =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    assertThat(exporter).isNotNull();
    FileUtils.deleteQuietly(new File("local.txt"));
    exporter.exportToFile("local.txt", false, 20);
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
          log.info(
              "block {}, txs {}, msgs {}",
              b.getSeqno(),
              b.getBlock().getAllTransactions().size(),
              b.getBlock().getAllMessages().size());
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
    log.info("block {}", latestBlock);
    Gson gson =
        new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(byte[].class, new ByteArrayToHexTypeAdapter())
            .create();
    log.info("blockGson {}", gson.toJson(latestBlock));
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
          log.info("blockId {}", blockId);
        });
  }

  @Test
  public void testExporterGetAccountByAddress() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    Address testAddress =
        Address.of("-1:6744E92C6F71C776FBBCEF299E31BF76F39C245CD56F2075B89C6A22026B4131");
    long startTime = System.currentTimeMillis();

    Account account = exporter.getAccountByAddress(testAddress);
    log.info("received account : {}ms", System.currentTimeMillis() - startTime);
    log.info("balance {}", Utils.formatNanoValue(account.getBalance()));

    assertThat(account).isNotNull();
  }
}
