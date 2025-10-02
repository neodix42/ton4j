package org.ton.ton4j.exporter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.adnl.globalconfig.TonGlobalConfig;
import org.ton.ton4j.tl.liteserver.responses.BlockData;
import org.ton.ton4j.tl.liteserver.responses.MasterchainInfo;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.tlb.BlockIdExt;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class TestPerformanceAdnlTonlibExporter {

  public static final String TON_DB_ROOT_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testPerformanceExporter() throws Exception {

    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
    long start = System.currentTimeMillis();

    Pair<BlockIdExt, Block> block = exporter.getLast();
    long end = System.currentTimeMillis();
    log.info("Direct TON DB: {} ms", end - start);
  }

  @Test
  public void testPerformanceAdnl() throws Exception {

    TonGlobalConfig tonGlobalConfig =
        TonGlobalConfig.loadFromUrl(Utils.getGlobalConfigUrlTestnetGithub());
    long start = System.currentTimeMillis();
    AdnlLiteClient client = AdnlLiteClient.builder().globalConfig(tonGlobalConfig).build();
    MasterchainInfo info = client.getMasterchainInfo();
    BlockData blockData = client.getBlock(info.getLast());
    Block block = blockData.getBlock();
    long end = System.currentTimeMillis();
    log.info("AdnlLiteClient: {} ms", end - start);
  }

  @Test
  public void testPerformanceTonlib() throws Exception {

    // tonlib does not have a method to return the whole deserialized Block
  }
}
