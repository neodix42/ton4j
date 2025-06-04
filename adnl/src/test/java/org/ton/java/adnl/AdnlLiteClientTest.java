package org.ton.java.adnl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ton.java.adnl.globalconfig.TonGlobalConfig;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.types.*;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class AdnlLiteClientTest {

  public static final String TON_FOUNDATION = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N";

  private static AdnlLiteClient client;
  private LiteClientConnectionPool pool;
  private static final String CONFIG_PATH = "testnet-global.config.json";

  @BeforeAll
  static void tearBeforeAll() throws Exception {
    client = new AdnlLiteClient();
    TonGlobalConfig tonGlobalConfig = TonGlobalConfig.loadFromPath(CONFIG_PATH);

    client.connect(tonGlobalConfig.getLiteservers()[1]);
  }

  @AfterAll
  static void tearDown() {
    if (client != null) {
      client.close();
    }
  }

  @Test
  void testMasterchainInfo() throws Exception {
    log.info("Testing single lite-server connection");

    assertTrue(client.isConnected(), "Client should be connected");

    // Test ping by getting masterchain info
    Utils.sleep(6);
    MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info, "Masterchain info should not be null");
    assertNotNull(info.getLast(), "Last block should not be null");

    log.info("Last block seqno: {} ", info.getLast().getSeqno());
    log.info("Workchain: {}", info.getLast().getWorkchain());
    log.info("Shard: {}", info.getLast().getShard());
    log.info("init.wc: {}", info.getInit().getWorkchain());

    assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");
  }

  @Test
  void testGetAccountState() throws Exception {
    log.info("Testing getAccountState");
    assertTrue(client.isConnected(), "Client should be connected");

    // Test ping by getting masterchain info
    Utils.sleep(6);
    MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info, "Masterchain info should not be null");
    assertNotNull(info.getLast(), "Last block should not be null");

    AccountState accountState = client.getAccountState(info.getLast(), Address.of(TON_FOUNDATION));
    log.info("accountState: {} ", accountState);
    log.info("Last block seqno: {} ", accountState.getId().getSeqno());
    log.info("shard block seqno: {} ", accountState.getShardblk().getSeqno());
    assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");
    log.info("accountStateObject: {} ", accountState.getAccountState());
  }

  //  @Test
  //  void testConnectionPool() throws Exception {
  //    log.info("Testing connection pool");
  //    TonGlobalConfig tonGlobalConfig = TonGlobalConfig.loadFromPath(CONFIG_PATH);

  //
  //    pool = new LiteClientConnectionPool();
  //    pool.addConnection(
  //        Utils.int2ip(tonGlobalConfig.getLiteservers()[0].getIp()),
  //        (int) tonGlobalConfig.getLiteservers()[0].getPort(),
  //        tonGlobalConfig.getLiteservers()[0].getId().getKey());
  //
  //    assertEquals(1, pool.getTotalConnectionCount(), "Should have 1 connection");
  //    assertEquals(1, pool.getActiveConnectionCount(), "Should have 1 active connection");
  //
  //    // Test query through pool
  //    //    MasterchainInfo info = pool.getMasterchainInfo();
  //    //    assertNotNull(info, "Masterchain info should not be null");
  //    //    log.info("Pool query successful - Last block seqno: " + info.getLast().getSeqno());
  //  }

  @Test
  void testGetTime() throws Exception {
    log.info("Testing getTime query");
    assertTrue(client.isConnected(), "Client should be connected");

    CurrentTime time = client.getTime();
    assertNotNull(time, "CurrentTime should not be null");
    assertTrue(time.getNow() > 0, "Now timestamp should be positive");

    log.info("Current time: {}", time.getNow());
  }

  @Test
  void testGetVersion() throws Exception {
    log.info("Testing getVersion query");
    assertTrue(client.isConnected(), "Client should be connected");

    Version version = client.getVersion();
    assertNotNull(version, "Version should not be null");
    assertTrue(version.getVersion() > 0, "Version number should be positive");
    assertTrue(version.getNow() > 0, "Now timestamp should be positive");

    log.info("Lite server version: {}", version.getVersion());
    log.info("Mode: {}", version.getMode());
    log.info("Capabilities: {}", version.getCapabilities());
    log.info("Now: {}", version.getNow());
    assertThat(version.getNow()).isGreaterThan(0);
  }

  @Test
  void testGetBlock() throws Exception {
    log.info("Testing testGetBlock query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    BlockData blockData = client.getBlock(masterchainInfo.getLast());
    log.info("getBlock {}", blockData);
    log.info("Block  {}", blockData.getBlock());
    assertThat(blockData.getId().getSeqno()).isGreaterThan(0);
    assertThat(blockData.getBlock()).isNotNull();
  }

  @Test
  void testGetConfigAll() throws Exception {
    log.info("Testing testGetBlock query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("block {}", masterchainInfo.getLast());
    //    BlockIdExt customBlock =
    //        BlockIdExt.builder()
    //            .seqno(31856756)
    //            .workchain(-1)
    //            .shard(-9223372036854775808L)
    //            .rootHash(
    //                Utils.hexToSignedBytes(
    //                    "2bf8ef2c056191e5ad256e1dc2b3540de82d53ffc0c125fef401917db39c522f"))
    //            .fileHash(
    //                Utils.hexToSignedBytes(
    //                    "c7d6883bb3dbc17cb704e6fe41ddbfdecb5574bda8bdaa8ffd3cb0825ca14d87"))
    //            .build();
    ConfigAll configAll = client.getConfigAll(masterchainInfo.getLast(), 0);
    log.info("configAll {}", configAll);
    assertThat(configAll.getId().getSeqno()).isGreaterThan(0);
    /* <pre>
    //
    // my                                                                           liteQuery    cfgAllQ
    // 7af98bb4 4319c2b9b2fd5f0fd3640134cce2d75cd9bbfe548df63cec84694e85f9230da95d df068c79 58 b7261b91         ffffffff00000000000000807418e6012bf8ef2c056191e5ad256e1dc2b3540de82d53ffc0c125fef401917db39c522fc7d6883bb3dbc17cb704e6fe41ddbfdecb5574bda8bdaa8ffd3cb0825ca14d87000000000000
    // go
    // 7af98bb4 23c1a5f4ec33591202d36572036fafedc5d137e2897d95a1dcc1aad8982c0f3960 df068c79 58 b7261b91 00000000ffffffff00000000000000807418e6012bf8ef2c056191e5ad256e1dc2b3540de82d53ffc0c125fef401917db39c522fc7d6883bb3dbc17cb704e6fe41ddbfdecb5574bda8bdaa8ffd3cb0825ca14d87000000000000
    // change 2
    // 7af98bb4 e2ae0f277e79bbdf43171ddcfa534ad81288d6adbd9a5e89d2b90bc35a371aa964 df068c79 58 b7261b91 00000000ffffffff00000000000000807418e6012bf8ef2c056191e5ad256e1dc2b3540de82d53ffc0c125fef401917db39c522fc7d6883bb3dbc17cb704e6fe41ddbfdecb5574bda8bdaa8ffd3cb0825ca14d87000000000000 000000000000000000000000
    </pre>
     */

  }

  @Test
  void testGetBlockHeader() throws Exception {
    log.info("Testing testGetBlockHeader query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    BlockHeader blockHeader = client.getBlockHeader(masterchainInfo.getLast(), 1);
    log.info("getBlockHeader {}", blockHeader); // todo review result
    log.info("Block  {}", blockHeader.getId());
    assertThat(blockHeader.getId().getSeqno()).isGreaterThan(0);
  }

  @Test
  void testGetBlockState() throws Exception {
    // cannot request total state: possibly too large todo
    log.info("Testing testGetBlockState query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    BlockState blockState = client.getBlockState(masterchainInfo.getLast());
    log.info("blockState {}", blockState);
    log.info("blockState.id  {}", blockState.getId());
    assertThat(blockState.getId().getSeqno()).isGreaterThan(0);
  }

  @Test
  void testGetShardInfo() throws Exception {
    log.info("Testing getShardInfo query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    ShardInfo shardInfo =
        client.getShardInfo(
            masterchainInfo.getLast(),
            masterchainInfo.getLast().getWorkchain(),
            masterchainInfo.getLast().shard,
            true); // Not enough data to read at 96 todo

    log.info("shardInfo {}", shardInfo);
  }

  @Test
  void testGetAllShardsInfo() throws Exception {
    log.info("Testing getAllShardsInfo query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    AllShardsInfo allShardInfo =
        client.getAllShardsInfo(masterchainInfo.getLast()); // Not enough data to read at 96 todo

    log.info("allShardInfo {}", allShardInfo);
  }

  @Test
  void testGetOneTransaction() throws Exception {
    log.info("Testing getOneTransaction query");
    assertTrue(client.isConnected(), "Client should be connected");

    // Placeholder for actual implementation
    log.info("getOneTransaction test completed");
  }

  @Test
  void testGetTransactions() throws Exception {
    log.info("Testing getTransactions query");
    assertTrue(client.isConnected(), "Client should be connected");

    // Placeholder for actual implementation
    log.info("getTransactions test completed");
  }

  @Test
  void testLookupBlock() throws Exception {
    log.info("Testing lookupBlock query");
    assertTrue(client.isConnected(), "Client should be connected");

    // Placeholder for actual implementation
    log.info("lookupBlock test completed");
  }

  @Test
  void testLookupBlockWithProof() throws Exception {
    log.info("Testing lookupBlockWithProof query");
    assertTrue(client.isConnected(), "Client should be connected");

    // Placeholder for actual implementation
    log.info("lookupBlockWithProof test completed");
  }

  @Test
  void testListBlockTransactions() throws Exception {
    log.info("Testing listBlockTransactions query");
    assertTrue(client.isConnected(), "Client should be connected");

    // Placeholder for actual implementation
    log.info("listBlockTransactions test completed");
  }

  @Test
  void testListBlockTransactionsExt() throws Exception {
    log.info("Testing listBlockTransactionsExt query");
    assertTrue(client.isConnected(), "Client should be connected");

    // Placeholder for actual implementation
    log.info("listBlockTransactionsExt test completed");
  }

  @Test
  void testGetBlockProof() throws Exception {
    log.info("Testing getBlockProof query");
    assertTrue(client.isConnected(), "Client should be connected");

    // Placeholder for actual implementation
    log.info("getBlockProof test completed");
  }

  // ... manual test method ...
}
