package org.ton.java.tonlib;

import static java.util.Objects.nonNull;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentTonlibTestnet {

  String tonlibPath = Utils.getTonlibGithubUrl();
  //  static String tonlibPath = "tonlibjson-prev.dll";

  private Tonlib tonlib = Tonlib.builder().testnet(false).pathToTonlibSharedLib(tonlibPath).build();

  @Test
  @ThreadCount(10)
  public void testTonlibRunMethod() throws InterruptedException {
    log.info("tonlib instance {}", tonlib);
    MasterChainInfo last = tonlib.getLast();
    log.info("last: {}", last);
    Thread.sleep(100);

    BlockIdExt initialBlock = tonlib.getMasterChainInfo().getLast();
    Shards shards = tonlib.getShards(initialBlock.getSeqno(), 0, 0);
    log.info("shards {}", shards);
    for (BlockIdExt shard : shards.getShards()) {
      BlockIdExt currentBlock =
          tonlib.lookupBlock(shard.getSeqno(), shard.getWorkchain(), shard.getShard(), 0);
      log.info("currentBlock {}", currentBlock);
      BlockTransactions txs = tonlib.getBlockTransactions(currentBlock, 10);
      log.info("txs {}", txs.getTransactions().size());
      if (nonNull(txs.getTransactions())) {
        for (ShortTxId tx : txs.getTransactions()) {
          String addressHex = Utils.base64ToHexString(tx.getAccount());
          log.info("addressHex {}", addressHex);
          RawTransaction extendedTransaction = tonlib.getRawTransaction((byte) 0, tx); // FAIL here
          log.info("tx is {}", extendedTransaction);
        }
      }
    }
  }
}
