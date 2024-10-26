package org.ton.java.liteclient;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ton.java.liteclient.api.ResultLastBlock;
import org.ton.java.liteclient.api.ResultListBlockTransactions;
import org.ton.java.liteclient.api.block.Block;
import org.ton.java.liteclient.api.block.Transaction;

/** Integration Tests that run against live testnet */
@Slf4j
public class LiteClientTest {
  private static final String CURRENT_DIR = System.getProperty("user.dir");

  private static LiteClient liteClient;

  @BeforeClass
  public static void executedBeforeEach() {

    liteClient = LiteClient.builder().testnet(true).build();
  }

  @Test
  public void testLastExecuted() {
    assertThat(liteClient.executeLast())
        .isNotNull()
        .contains("last masterchain block is")
        .contains("server time is");
  }

  @Test
  public void testRunMethod() throws Exception {
    final String result =
        liteClient.executeRunMethod(
            "EQDCJVrezD71y-KPcTIG-YeKNj4naeiR7odpQgVA1uDsZqPC", "seqno", "");
    log.info(result);
    assertThat(result).contains("arguments").contains("result");
  }

  @Test
  public void testRunMethodWithBlockId() throws Exception {
    final String result =
        liteClient.executeRunMethod(
            "EQDCJVrezD71y-KPcTIG-YeKNj4naeiR7odpQgVA1uDsZqPC",
            "(-1,8000000000000000,20301499):070D07EB64D36CCA2D8D20AA644489637059C150E2CD466247C25B4997FB8CD9:D7D7271D466D52D0A98771F9E8DCAA06E43FCE01C977AACD9DE9DAD9A9F9A424",
            "seqno",
            "");
    log.info(result);
    assertThat(result).contains("arguments").contains("result");
  }

  @Test
  public void testRunMethodWithResultBlockId() throws Exception {
    ResultLastBlock blockId =
        ResultLastBlock.builder()
            .wc(-1L)
            .shard("8000000000000000")
            .seqno(BigInteger.valueOf(20301499))
            .rootHash("070D07EB64D36CCA2D8D20AA644489637059C150E2CD466247C25B4997FB8CD9")
            .fileHash("D7D7271D466D52D0A98771F9E8DCAA06E43FCE01C977AACD9DE9DAD9A9F9A424")
            .build();
    final String result =
        liteClient.executeRunMethod(
            "EQDCJVrezD71y-KPcTIG-YeKNj4naeiR7odpQgVA1uDsZqPC", blockId, "seqno", "");
    log.info(result);
    assertThat(result).contains("arguments").contains("result");
  }

  @Test
  public void testSendfile() throws Exception {
    final InputStream bocFile =
        IOUtils.toBufferedInputStream(getClass().getResourceAsStream("/new-wallet.boc"));
    final File targetFile = new File(CURRENT_DIR + File.separator + "new-wallet.boc");
    FileUtils.copyInputStreamToFile(bocFile, targetFile);
    final String result = liteClient.executeSendfile(targetFile.getAbsolutePath());
    log.info(result);
    assertThat(result).contains("sending query from file").contains("external message status is 1");
  }

  @Test
  public void testListblocktransExecuted() {
    // given
    String resultLast = liteClient.executeLast();
    log.info("testListblocktransExecuted resultLast received");
    ResultLastBlock resultLastBlock = LiteClientParser.parseLast(resultLast);
    log.info("testListblocktransExecuted tonBlockId {}", resultLastBlock);
    // when
    String stdout = liteClient.executeListblocktrans(resultLastBlock, 2000);
    System.out.println(stdout);
    // then
    assertThat(stdout)
        .isNotNull()
        .contains("last masterchain block is")
        .contains("obtained block")
        .contains("transaction #")
        .contains("account")
        .contains("hash");
  }

  @Test
  public void testAllShardsExecuted() throws Exception {
    // given
    String resultLast = liteClient.executeLast();
    log.info("testAllShardsExecuted resultLast received");
    assertThat(resultLast).isNotEmpty();
    ResultLastBlock resultLastBlock = LiteClientParser.parseLast(resultLast);
    // when
    String stdout = liteClient.executeAllshards(resultLastBlock);
    // then
    assertThat(stdout)
        .isNotNull()
        .contains("last masterchain block is")
        .contains("obtained block")
        .contains("got shard configuration with respect to block")
        .contains("shard configuration is")
        .contains("shard #");
  }

  @Test
  public void testParseBySeqno() throws Exception {
    // given
    // 9MB size block
    // (0,f880000000000000,4166691):6101667C299D3DD8C9E4C68F0BCEBDBA5473D812953C291DBF6D69198C34011B:608F5FC6D6CFB8D01A3D4A2F9EA5C353D82B4A08D7D755D8267D0141358329F1
    String resultLast = liteClient.executeLast();
    assertThat(resultLast).isNotEmpty();
    ResultLastBlock blockIdLast = LiteClientParser.parseLast(resultLast);
    assertThatObject(blockIdLast).isNotNull();
    assertNotNull(blockIdLast.getRootHash());
    // when
    String stdout =
        liteClient.executeBySeqno(
            blockIdLast.getWc(), blockIdLast.getShard(), blockIdLast.getSeqno());
    log.info(stdout);
    ResultLastBlock blockId = LiteClientParser.parseBySeqno(stdout);
    // then
    assertEquals(-1L, blockId.getWc().longValue());
    assertNotEquals(0L, blockId.getShard());
    assertNotEquals(0L, blockId.getSeqno().longValue());
  }

  @Test
  public void testDumpBlockRealTimeExecuted() {
    log.info(
        "testDumpBlockRealTimeExecuted test executes against the most recent state of TON blockchain, if it fails means the return format has changed - react asap.");
    // given
    String resultLast = liteClient.executeLast();
    log.info("testDumpBlockRealTimeExecuted resultLast received");
    assertThat(resultLast).isNotEmpty();
    ResultLastBlock resultLastBlock = LiteClientParser.parseLast(resultLast);
    log.info("testDumpBlockRealTimeExecuted tonBlockId {}", resultLastBlock);

    // when
    String stdout = liteClient.executeDumpblock(resultLastBlock);
    log.info(stdout);
    // then
    assertThat(stdout)
        .isNotNull()
        .contains("last masterchain block is")
        .contains("got block download request for")
        .contains("block header of")
        .contains("block contents is (block global_id")
        .contains("state_update:(raw@(MERKLE_UPDATE")
        .contains("extra:(block_extra")
        .contains("shard_fees:(")
        .contains("x{11EF55AAFFFFFF");
  }

  @Test
  public void testParseLastParsed() {
    // given
    String stdout = liteClient.executeLast();
    assertNotNull(stdout);
    // when
    ResultLastBlock blockId = LiteClientParser.parseLast(stdout);
    // then
    assertNotNull(blockId);
    assertNotNull(blockId.getFileHash());
    assertNotNull(blockId.getRootHash());
    assertEquals(-1L, blockId.getWc().longValue());
    assertNotEquals(0L, blockId.getShard());
    assertNotEquals(0L, blockId.getSeqno().longValue());
  }

  @Test
  public void testParseListBlockTrans() {
    // given
    String stdoutLast = liteClient.executeLast();
    // when
    assertNotNull(stdoutLast);
    ResultLastBlock blockIdLast = LiteClientParser.parseLast(stdoutLast);

    String stdoutListblocktrans = liteClient.executeListblocktrans(blockIdLast, 0);
    log.info(stdoutListblocktrans);
    // then
    assertNotNull(stdoutListblocktrans);
    List<ResultListBlockTransactions> txs =
        LiteClientParser.parseListBlockTrans(stdoutListblocktrans);
    txs.forEach(System.out::println);
    assertEquals(BigInteger.ONE, txs.get(0).getTxSeqno());
  }

  @Test
  public void testParseAllShards() throws Exception {
    // given
    String stdoutLast = liteClient.executeLast();
    // when
    assertNotNull(stdoutLast);
    ResultLastBlock blockIdLast = LiteClientParser.parseLast(stdoutLast);
    String stdoutAllShards = liteClient.executeAllshards(blockIdLast);
    log.info(stdoutAllShards);
    // then
    assertNotNull(stdoutAllShards);
    List<ResultLastBlock> shards = LiteClientParser.parseAllShards(stdoutAllShards);

    shards.forEach(System.out::println);
    assertTrue(shards.get(0).getSeqno().longValue() > 0);
  }

  @Test
  public void testParseDumptransNew() {
    // given
    String stdoutLast = liteClient.executeLast();
    assertNotNull(stdoutLast);
    ResultLastBlock blockIdLast = LiteClientParser.parseLast(stdoutLast);

    String stdoutListblocktrans = liteClient.executeListblocktrans(blockIdLast, 0);
    assertNotNull(stdoutListblocktrans);
    log.info(stdoutListblocktrans);
    List<ResultListBlockTransactions> txs =
        LiteClientParser.parseListBlockTrans(stdoutListblocktrans);

    for (ResultListBlockTransactions tx : txs) {
      String stdoutDumptrans = liteClient.executeDumptrans(blockIdLast, tx);
      assertNotNull(stdoutDumptrans);
      Transaction txdetails = LiteClientParser.parseDumpTrans(stdoutDumptrans, true);
      if (!isNull(txdetails)) {
        log.info(txdetails.toString());
        assertNotEquals(0, txdetails.getLt().longValue());
      }
    }
  }

  @Test
  public void testParseAllSteps() throws Exception {
    // given
    String stdoutLast = liteClient.executeLast();
    assertNotNull(stdoutLast);
    ResultLastBlock blockIdLast = LiteClientParser.parseLast(stdoutLast);

    String stdoutAllShards = liteClient.executeAllshards(blockIdLast);
    // log.info(stdoutAllShards);

    String stdoutListblocktrans = liteClient.executeListblocktrans(blockIdLast, 0);
    assertNotNull(stdoutListblocktrans);
    log.info(stdoutListblocktrans);
    List<ResultListBlockTransactions> txs =
        LiteClientParser.parseListBlockTrans(stdoutListblocktrans);

    // then
    assertNotNull(stdoutAllShards);
    List<ResultLastBlock> shards = LiteClientParser.parseAllShards(stdoutAllShards);
    for (ResultLastBlock shard : shards) {
      String stdoutListblocktrans2 = liteClient.executeListblocktrans(shard, 0);
      List<ResultListBlockTransactions> txs2 =
          LiteClientParser.parseListBlockTrans(stdoutListblocktrans2);
      txs.addAll(txs2);
    }

    txs.forEach(System.out::println);

    for (ResultListBlockTransactions tx : txs) {
      String stdoutDumptrans = liteClient.executeDumptrans(blockIdLast, tx);
      assertNotNull(stdoutDumptrans);
      Transaction txdetails = LiteClientParser.parseDumpTrans(stdoutDumptrans, true);
      if (!isNull(txdetails)) {
        assertNotEquals(0, txdetails.getLt().longValue());
      }
    }
  }

  @Test
  public void testParseDumpblock() throws Exception {
    // given
    String stdoutLast = liteClient.executeLast();
    assertNotNull(stdoutLast);
    ResultLastBlock blockIdLast = LiteClientParser.parseLast(stdoutLast);
    String stdoutDumpblock = liteClient.executeDumpblock(blockIdLast);

    Block block = LiteClientParser.parseDumpblock(stdoutDumpblock, false, false);
    assertNotNull(block);

    assertNotEquals(0L, block.getGlobalId().longValue());
    assertNotEquals(0L, block.getInfo().getSeqNo().longValue());
    assertNotNull(block.getInfo().getPrevFileHash());
    block
        .listBlockTrans()
        .forEach(
            x ->
                log.info(
                    "account: {} lt: {} hash: {}", x.getAccountAddr(), x.getLt(), x.getNewHash()));

    block
        .allShards()
        .forEach(
            x ->
                log.info(
                    "wc: {} shard: {}, seqno: {} root_hash: {} file_hash: {} utime: {}, start_lt: {} end_lt: {}",
                    x.getWc(),
                    new BigInteger(x.getNextValidatorShard()).toString(16),
                    x.getSeqno(),
                    x.getRootHash(),
                    x.getFileHash(),
                    x.getGenUtime(),
                    x.getStartLt(),
                    x.getEndLt()));
  }
}
