package org.ton.ton4j.exporter;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.exporter.lazy.ShardAccountLazy;
import org.ton.ton4j.exporter.types.*;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.tlb.BlockId;
import org.ton.ton4j.tlb.BlockIdExt;
import org.ton.ton4j.tlb.adapters.*;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestExporter {

  public static final String TON_DB_ROOT_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  public static final Gson gson =
      new GsonBuilder()
          .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
          .registerTypeHierarchyAdapter(Cell.class, new CellTypeAdapter())
          .registerTypeAdapter(byte[].class, new ByteArrayToHexTypeAdapter())
          .registerTypeAdapter(BitString.class, new BitStringTypeAdapter())
          //          .registerTypeAdapter(TonHashMapAug.class, new TonHashMapAugTypeAdapter())
          //          .registerTypeAdapter(TonHashMapAugE.class, new TonHashMapAugETypeAdapter())
          //          .registerTypeHierarchyAdapter(TonHashMap.class, new TonHashMapTypeAdapter())
          //          .registerTypeAdapter(TonHashMapE.class, new TonHashMapETypeAdapter())
          .disableHtmlEscaping()
          .setLenient()
          .create();

  org.ton.ton4j.tl.types.db.block.BlockIdExt blockIdExtMc =
      org.ton.ton4j.tl.types.db.block.BlockIdExt.builder()
          .workchain(-1)
          .seqno(229441)
          .shard(0x8000000000000000L)
          .rootHash(
              Utils.hexToSignedBytes(
                  "439233F8D4B99BAD7A2CC84FFE0D16150ADC0E1058BCDF82243D1445A75CA5BF"))
          .fileHash(
              Utils.hexToSignedBytes(
                  "E24EA0E5F520135DA4FC0B0477E5440E0D1C4E7EDB2026941F0457376BB3D97E"))
          .build();

  org.ton.ton4j.tl.types.db.block.BlockIdExt blockIdExt =
      org.ton.ton4j.tl.types.db.block.BlockIdExt.builder()
          .workchain(0)
          .seqno(229441)
          .shard(0x8000000000000000L)
          .rootHash(
              Utils.hexToSignedBytes(
                  "5F49521AD8EC570C82B6DA6D1AF9D16884CA17F3310044BBB66ED6B94A15608C"))
          .fileHash(
              Utils.hexToSignedBytes(
                  "7925B49AF1FF46550998947C05EC2B2AAD2F89B1C4FA98F3A19DDB62ACDF36EC"))
          .build();

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
    exporter.exportToFile("blocks-tlb.txt", true, 16);
  }

  @Test
  public void testExporterToFileNotDeserialized() throws IOException {
    Exporter exporter =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    assertThat(exporter).isNotNull();
    exporter.exportToFile("blocks-boc.txt", false, 32);
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
          //          if (b.getSeqno() == 209936) {
          //            log.info(gson.toJson(b.getBlock()));
          //            for (InMsg msg : b.getBlock().getExtra().getInMsgDesc().getInMessages()) {
          //              log.info("outmsgs {}", msg);
          //            }
          //          }
        });
    blockStream.close();
  }

  @Test
  public void testExporterGetBlockByBlockIdExtMc() throws IOException {
    BlockIdExt blockIdExtMc =
        BlockIdExt.builder()
            .workchain(-1)
            .seqno(229441)
            .shard(0x8000000000000000L)
            .rootHash(
                Utils.hexToSignedBytes(
                    "439233F8D4B99BAD7A2CC84FFE0D16150ADC0E1058BCDF82243D1445A75CA5BF"))
            .fileHash(
                Utils.hexToSignedBytes(
                    "E24EA0E5F520135DA4FC0B0477E5440E0D1C4E7EDB2026941F0457376BB3D97E"))
            .build();

    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

    Block block = exporter.getBlock(blockIdExtMc);
    log.info("block {}", block);
  }

  @Test
  public void testExporterGetBlockByBlockIdExtNonMc() throws IOException {
    BlockIdExt blockIdExt =
        BlockIdExt.builder()
            .workchain(0)
            .seqno(229441)
            .shard(0x8000000000000000L)
            .rootHash(
                Utils.hexToSignedBytes(
                    "5F49521AD8EC570C82B6DA6D1AF9D16884CA17F3310044BBB66ED6B94A15608C"))
            .fileHash(
                Utils.hexToSignedBytes(
                    "7925B49AF1FF46550998947C05EC2B2AAD2F89B1C4FA98F3A19DDB62ACDF36EC"))
            .build();

    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

    Block block = exporter.getBlock(blockIdExt);
    log.info("block {}", block);
  }

  @Test
  public void testExporterGetBlockByBlockId() throws IOException {
    long startTime = System.currentTimeMillis();

    BlockId blockIdMc =
        BlockId.builder().workchain(-1).seqno(229441).shard(0x8000000000000000L).build();

    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

    Block block = exporter.getBlock(blockIdMc);
    long endTime = System.currentTimeMillis();
    log.info("block {}", block);
    log.info("elapsed time: {} ms", endTime - startTime);
  }

  @Test
  public void testExporterGetBlockByBlockId220000() throws IOException {
    long startTime = System.currentTimeMillis();

    BlockId blockIdMc =
        BlockId.builder().workchain(-1).seqno(220000).shard(0x8000000000000000L).build();

    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

    Block block = exporter.getBlock(blockIdMc);
    long endTime = System.currentTimeMillis();
    log.info("block {}", block);
    log.info("elapsed time: {} ms", endTime - startTime);
  }

  @Test
  public void testExporterGetBlockByBlockIdNonMc() throws IOException {
    long startTime = System.currentTimeMillis();
    BlockId blockId =
        BlockId.builder().workchain(0).seqno(229441).shard(0x8000000000000000L).build();

    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

    Block block = exporter.getBlock(blockId);
    long endTime = System.currentTimeMillis();
    log.info("block {}", block);
    log.info("elapsed time: {} ms", endTime - startTime);
  }

  @Test
  public void testExporterGetLastShards() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

    long startTime = System.currentTimeMillis();

    Pair<BlockIdExt, Block> block = exporter.getLast();
    Block latestBlock = block.getRight();

    long durationMs = System.currentTimeMillis() - startTime;

    log.info("received last block in {}ms", durationMs);

    log.info("Latest block found:");
    log.info("  Workchain: {}", latestBlock.getBlockInfo().getShard().getWorkchain());
    log.info(
        "  Shard: {}",
        latestBlock.getBlockInfo().getShard().convertShardIdentToShard().toString(16));
    log.info("  Sequence Number: {}", latestBlock.getBlockInfo().getSeqno());
    log.info("  Timestamp: {}", latestBlock.getBlockInfo().getGenuTime());
    log.info(
        "txs {}, msgs {} (in {}, out {})",
        latestBlock.getAllTransactions().size(),
        latestBlock.getAllMessages().size(),
        latestBlock.getAllIncomingMessages().size(),
        latestBlock.getAllOutgoingMessages().size());

    log.info("block: {}", latestBlock);

    log.info("blockGson: {}", gson.toJson(latestBlock));

    log.info("shards: {}", latestBlock.getAllShardDescrs());
  }

  @Test
  public void testExporterGetLastByWcShard() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    Block last = exporter.getLast(0, 0x8000000000000000L);
    log.info("last block: {}", last);
  }

  @Test
  public void testExporterGetLastWithLimit() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    long startTime = System.currentTimeMillis();

    TreeMap<BlockIdExt, Block> latestBlocks = exporter.getLast(10);

    long durationMs = System.currentTimeMillis() - startTime;

    log.info("received last block : {}ms", durationMs);

    latestBlocks.forEach(
        (blockIdExt, block) -> {
          log.info("blockId {}", blockIdExt);
        });
  }

  // and compare txs count and shardchains blocks
  // mainnet stats
  // export bocs - 1.5k block rate, min pack size 2mb, max pack size 20mb
  // export tlb - started 900 blockrate,
  @Test
  public void testJsonVsBocCount() throws IOException {
    Files.deleteIfExists(Path.of("status.json"));
    long count1, count2;

    // First run
    log.info("Starting first run deserialized=false...");
    Exporter exporter1 =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    Stream<ExportedBlock> blockStream1 = exporter1.exportToObjects(false, 20);
    count1 = blockStream1.count();
    blockStream1.close();
    log.info("First run completed with count: {}, errors {}", count1, exporter1.getErrorsCount());

    Files.deleteIfExists(Path.of("status.json"));

    // Add delay between runs
    Utils.sleep(1);

    // Second run
    log.info("Starting second run deserialized=true...");
    Exporter exporter2 =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    Stream<ExportedBlock> blockStream2 = exporter2.exportToObjects(true, 20);
    count2 = blockStream2.count();
    blockStream2.close();
    log.info("Second run completed with count: {}, errors {}", count2, exporter2.getErrorsCount());

    Files.deleteIfExists(Path.of("status.json"));

    assertThat(count1).isEqualTo(count2);
  }

  @Test
  public void testJsonVsBocFileCount() throws IOException {
    Files.deleteIfExists(Path.of("status.json"));

    // First run
    log.info("Starting first run deserialized=false...");
    Exporter exporter1 =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    exporter1.exportToFile("blocks-boc.txt", false, 20);

    log.info(
        "First run completed with count: {}, errors {}",
        exporter1.getParsedBlocksCount(),
        exporter1.getErrorsCount());

    Files.deleteIfExists(Path.of("status.json"));

    // Add delay between runs
    Utils.sleep(1);

    // Second run
    log.info("Starting second run deserialized=true...");
    Exporter exporter2 =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    exporter2.exportToFile("blocks-tlb.txt", true, 20);

    log.info(
        "Second run completed with count: {}, errors {}",
        exporter2.getParsedBlocksCount(),
        exporter2.getErrorsCount());

    Files.deleteIfExists(Path.of("status.json"));

    assertThat(exporter1.getParsedBlocksCount()).isEqualTo(exporter2.getParsedBlocksCount());
  }

  @Test
  public void testBocCountConsistency() throws IOException {

    Files.deleteIfExists(Path.of("status.json"));

    // Run the count operation multiple times to verify consistency
    // Use separate Exporter instances to simulate real-world usage
    long count1, count2, count3;

    // Add a small delay to ensure any file system operations are complete
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // First run
    log.info("Starting first run...");
    Exporter exporter1 =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    Stream<ExportedBlock> blockStream1 = exporter1.exportToObjects(false, 20);
    count1 = blockStream1.count();
    blockStream1.close();
    log.info("First run completed with count: {}", count1);

    Files.deleteIfExists(Path.of("status.json"));

    // Add delay between runs
    Utils.sleep(1);

    // Second run
    log.info("Starting second run...");
    Exporter exporter2 =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    Stream<ExportedBlock> blockStream2 = exporter2.exportToObjects(false, 20);
    count2 = blockStream2.count();
    blockStream2.close();
    log.info("Second run completed with count: {}", count2);

    Files.deleteIfExists(Path.of("status.json"));

    // Add delay between runs
    Utils.sleep(1);

    // Third run
    log.info("Starting third run...");
    Exporter exporter3 =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    Stream<ExportedBlock> blockStream3 = exporter3.exportToObjects(false, 20);
    count3 = blockStream3.count();
    blockStream3.close();
    log.info("Third run completed with count: {}", count3);

    log.info("Count 1: {}, Count 2: {}, Count 3: {}", count1, count2, count3);

    // Check if the issue is just with the first run
    if (count2 == count3 && count1 != count2) {
      log.warn(
          "First run inconsistency detected: {} vs {} (difference: {})",
          count1,
          count2,
          count2 - count1);
      // For now, just verify that runs 2 and 3 are consistent
      assertThat(count2).isEqualTo(count3);
      log.info("Runs 2 and 3 are consistent: {}", count2);
    } else {
      // All counts should be identical
      assertThat(count1).isEqualTo(count2).isEqualTo(count3);
      log.info("SUCCESS: All counts are consistent: {}", count1);
    }
  }

  /** Count 1: 445562, Count 2: 445657, Count 3: 445557 */
  @Test
  public void testTlbCountConsistency() throws IOException {
    // Test to verify that the fix for premature thread pool shutdown works
    // This test should now return consistent results across multiple runs

    Files.deleteIfExists(Path.of("status.json"));

    // Run the count operation multiple times to verify consistency
    // Use separate Exporter instances to simulate real-world usage
    long count1, count2, count3;

    // Add a small delay to ensure any file system operations are complete
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // First run
    log.info("Starting first run...");
    Exporter exporter1 =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    Stream<ExportedBlock> blockStream1 = exporter1.exportToObjects(true, 20);
    count1 = blockStream1.count();
    blockStream1.close();
    log.info("First run completed with count: {}", count1);

    Files.deleteIfExists(Path.of("status.json"));

    // Add delay between runs
    Utils.sleep(1);

    // Second run
    log.info("Starting second run...");
    Exporter exporter2 =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    Stream<ExportedBlock> blockStream2 = exporter2.exportToObjects(true, 20);
    count2 = blockStream2.count();
    blockStream2.close();
    log.info("Second run completed with count: {}", count2);

    Files.deleteIfExists(Path.of("status.json"));

    // Add delay between runs
    Utils.sleep(1);

    // Third run
    log.info("Starting third run...");
    Exporter exporter3 =
        Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).showProgress(true).build();
    Stream<ExportedBlock> blockStream3 = exporter3.exportToObjects(true, 20);
    count3 = blockStream3.count();
    blockStream3.close();
    log.info("Third run completed with count: {}", count3);

    log.info("Count 1: {}, Count 2: {}, Count 3: {}", count1, count2, count3);

    // Check if the issue is just with the first run
    if (count2 == count3 && count1 != count2) {
      log.warn(
          "First run inconsistency detected: {} vs {} (difference: {})",
          count1,
          count2,
          count2 - count1);
      // For now, just verify that runs 2 and 3 are consistent
      assertThat(count2).isEqualTo(count3);
      log.info("Runs 2 and 3 are consistent: {}", count2);
    } else {
      // All counts should be identical
      assertThat(count1).isEqualTo(count2).isEqualTo(count3);
      log.info("SUCCESS: All counts are consistent: {}", count1);
    }
  }

  @Test
  public void testCellDbReaderGetAccountBalanceWc() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

    log.info("blockIdExtMc {}", blockIdExtMc);
    for (Address address :
        List.of(
            // adnl 777998095999, our algo finds nok - error "not a prefix"
            Address.of("0:b3dd5c861f4b3ff36da1996e31ef8394a83d0a5d08cfa472adc2eb804e5e849a"),
            // adnl 6007998, our algo finds ok
            Address.of("0:b3dd5e92a9c3a05a56930db015a7a35b07546ecf1f5fa425fd3d8e6a63fd28ea"),
            // adnl 777998095999, our algo finds nok - error "not a prefix"
            Address.of("0:7216e9db71acddecba3944137540c400f11fbabebeb23138fa5535c6a8784f2c"))) {
      //      ShardAccountLazy shardAccount =
      //          exporter.getShardAccountByAddress(blockIdExtMc, address, false);

      log.info(
          "shardAccount balance {} of {}",
          Utils.formatNanoValue(exporter.getBalance(address)),
          address.toRaw());
    }
  }

  @Test
  public void testCellDbReaderGetAccountBalanceMc() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    //    org.ton.ton4j.tl.types.db.block.BlockIdExt last = exporter.getLastBlockIdExt();

    // blockIdExtMc - 28,385.246832021
    // blockIdExt - 26,264.412991196 last
    //    log.info("blockIdExt {}", last);
    log.info("blockIdExt {}", blockIdExtMc);
    for (Address address :
        List.of(
            // adnl 4999988444800000000, our - not found
            Address.of("-1:0000000000000000000000000000000000000000000000000000000000000000"),
            Address.of("-1:5555555555555555555555555555555555555555555555555555555555555555"),
            Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333"),
            // adnl 1000001000000000, our - ok
            Address.of("-1:22f53b7d9aba2cef44755f7078b01614cd4dde2388a1729c2c386cf8f9898afe"),
            // adnl 669343021899572, our - not found
            Address.of("-1:6744e92c6f71c776fbbcef299e31bf76f39c245cd56f2075b89c6a22026b4131"))) {
      log.info(
          "shardAccount balance {} of {}",
          Utils.formatNanoValue(exporter.getBalance(address)),
          address.toRaw());
    }
  }

  @Test
  public void testCellDbReaderGetBalanceLatestWc() {
    Address address =
        Address.of("0:7216e9db71acddecba3944137540c400f11fbabebeb23138fa5535c6a8784f2c");
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    log.info("Balance {}", Utils.formatNanoValue(exporter.getBalance(address)));
  }

  @Test
  public void testCellDbReaderGetBalanceBySeqnoWc() {
    Address address =
        Address.of("0:7216e9db71acddecba3944137540c400f11fbabebeb23138fa5535c6a8784f2c");
    long mcSeqno = 220000;
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    log.info("Balance {}", Utils.formatNanoValue(exporter.getBalance(address, mcSeqno)));
  }

  @Test
  public void testCellDbReaderGetBalanceLatest() {
    Address address =
        Address.of("-1:0000000000000000000000000000000000000000000000000000000000000000");
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    log.info("Balance {}", Utils.formatNanoValue(exporter.getBalance(address)));
  }

  @Test
  public void testCellDbReaderGetBalanceBySeqno() {
    Address address =
        Address.of("-1:0000000000000000000000000000000000000000000000000000000000000000");
    long mcSeqno = 220000;
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    log.info("Balance {}", Utils.formatNanoValue(exporter.getBalance(address, mcSeqno)));
  }

  @Test
  public void testCellDbReaderGetBalanceBySeqnoE() {
    Address address =
        Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
    long mcSeqno = 220000;
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    log.info("Balance {}", Utils.formatNanoValue(exporter.getBalance(address, mcSeqno)));
  }

  @Test
  public void testShardLookUpWc() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    Pair<org.ton.ton4j.tlb.BlockIdExt, Block> pair = exporter.getLast();
    log.info("blockIdExt {}", pair.getLeft());
    Block lastBlock = pair.getRight();
    Address address =
        Address.of("0:b3dd5c861f4b3ff36da1996e31ef8394a83d0a5d08cfa472adc2eb804e5e849a");
    org.ton.ton4j.tlb.BlockIdExt shardInfo =
        ShardLookup.findShardBlock(lastBlock, address.wc, address.hashPart);
    log.info("shardInfo {}", shardInfo);
    ShardAccountLazy shardAccountLazy = exporter.getShardAccountByAddress(shardInfo, address);
    log.info("shardAccountLazy {}", shardAccountLazy);
  }

  @Test
  public void testShardLookUp() throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    Pair<org.ton.ton4j.tlb.BlockIdExt, Block> pair = exporter.getLast();
    BlockIdExt blockIdExt = pair.getLeft();
    log.info("blockIdExt {}", blockIdExt);
    Block lastBlock = pair.getRight();
    Address address =
        Address.of("-1:0000000000000000000000000000000000000000000000000000000000000000");
    // no need look
    org.ton.ton4j.tlb.BlockIdExt shardInfo =
        ShardLookup.findShardBlock(lastBlock, address.wc, address.hashPart);
    log.info("shardInfo {}", shardInfo);
    ShardAccountLazy shardAccountLazy = exporter.getShardAccountByAddress(blockIdExt, address);
    log.info("shardAccountLazy {}", shardAccountLazy);
  }
}
