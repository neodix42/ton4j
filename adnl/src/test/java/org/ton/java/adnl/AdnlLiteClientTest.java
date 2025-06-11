package org.ton.java.adnl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ton.java.adnl.globalconfig.TonGlobalConfig;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.liteserver.responses.*;
import org.ton.ton4j.tlb.Transaction;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class AdnlLiteClientTest {

  public static final String TESTNET_ADDRESS = "0QAyni3YDAhs7c-7imWvPyEbMEeVPMX8eWDLQ5GUe-B-Bl9Z";
  public static final String MAINNET_ADDRESS = "EQCRGnccIFznQqxm_oBm8PHz95iOe89Oe6hRAhSlAaMctuo6";
  public static final String ELECTOR_ADDRESS =
      "-1:3333333333333333333333333333333333333333333333333333333333333333";

  private static AdnlLiteClient client;
  private LiteClientConnectionPool pool;
  private static final boolean mainnet = true;

  @BeforeAll
  static void tearBeforeAll() throws Exception {

    TonGlobalConfig tonGlobalConfig;
    if (mainnet) {
      tonGlobalConfig = TonGlobalConfig.loadFromUrl(Utils.getGlobalConfigUrlMainnetGithub());
    } else {
      tonGlobalConfig = TonGlobalConfig.loadFromUrl(Utils.getGlobalConfigUrlTestnetGithub());
    }
    client =
        AdnlLiteClient.builder()
            .globalConfig(tonGlobalConfig)
            //            .liteServerIndex(2)
            .build();
  }

  @AfterAll
  static void tearDown() {
    if (client != null) {
      client.close();
    }
  }

  public static String getAddress() {
    if (mainnet) {
      return MAINNET_ADDRESS;
    }
    return TESTNET_ADDRESS;
  }

  @Test
  void testMasterchainInfo() throws Exception {
    log.info("Testing single lite-server connection");

    assertTrue(client.isConnected(), "Client should be connected");

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
  void testMasterchainInfoExt() throws Exception {
    log.info("Testing single lite-server connection");

    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfoExt infoExt = client.getMasterchainInfoExt(0);
    assertNotNull(infoExt, "Masterchain info should not be null");
    assertNotNull(infoExt.getLast(), "Last block should not be null");

    log.info("Last block seqno: {} ", infoExt.getLast().getSeqno());
    log.info("Workchain: {}", infoExt.getLast().getWorkchain());
    log.info("Shard: {}", infoExt.getLast().getShard());
    log.info("init.wc: {}", infoExt.getInit().getWorkchain());

    assertTrue(infoExt.getLast().getSeqno() > 0, "Seqno should be positive");
  }

  @Test
  void testGetBalance() throws Exception {
    log.info("Testing getBalance");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info(
        "account balance: {} ", Utils.formatNanoValue(client.getBalance(Address.of(getAddress()))));
  }

  @Test
  void testGetAccountState() throws Exception {
    log.info("Testing getAccountState");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info, "Masterchain info should not be null");
    assertNotNull(info.getLast(), "Last block should not be null");

    AccountState accountState = client.getAccountState(info.getLast(), Address.of(getAddress()));
    log.info("accountState: {} ", accountState);
    log.info("Last block seqno: {} ", accountState.getId().getSeqno());
    log.info("shard block seqno: {} ", accountState.getShardblk().getSeqno());
    log.info("accountHex: {} ", Address.of(getAddress()).toRaw());
    log.info("accountBalance: {} ", accountState.getAccount().getAccountStorage().getBalance());
    assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");
    log.info("accountObject: {} ", accountState.getAccount());
    //    log.info("accountShardObject: {} ", accountState.getShardAccount());
    //    log.info("ShardState: {} ", accountState.getShardState());
    log.info("ShardStateUnsplit: {} ", accountState.getShardStateUnsplit());
    log.info("getShardAccounts: {} ", accountState.getShardAccounts());
  }

  @Test
  void testGetAccountStatePruned() throws Exception {
    log.info("Testing getAccountStatePruned");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info, "Masterchain info should not be null");
    assertNotNull(info.getLast(), "Last block should not be null");

    AccountState accountState =
        client.getAccountStatePruned(info.getLast(), Address.of(getAddress()));
    log.info("accountState: {} ", accountState);
    log.info("Last block seqno: {} ", accountState.getId().getSeqno());
    log.info("shard block seqno: {} ", accountState.getShardblk().getSeqno());
    assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");
    log.info("accountObject: {} ", accountState.getAccount());
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
    log.info("Testing testConfigAll query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    ConfigInfo configInfo = client.getConfigAll(masterchainInfo.getLast(), 0);
    log.info("configAll {}", configInfo);
    assertThat(configInfo.getId().getSeqno()).isGreaterThan(0);
    log.info("configParsed {}", configInfo.getConfigParsed());
  }

  @Test
  void testGetConfigParams() throws Exception {
    log.info("Testing testConfigParams query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    ConfigInfo configInfo = client.getConfigParams(masterchainInfo.getLast(), 0, new int[] {32});
    log.info("configInfo {}", configInfo);
    assertThat(configInfo.getId().getSeqno()).isGreaterThan(0);
    log.info("configParsed {}", configInfo.getConfigParsed());
  }

  @Test
  void testGetBlockHeader() throws Exception {
    log.info("Testing testGetBlockHeader query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    BlockHeader blockHeader = client.getBlockHeader(masterchainInfo.getLast(), 3);
    log.info("getBlockHeader {}", blockHeader);
    assertThat(blockHeader.getId().getSeqno()).isGreaterThan(0);
  }

  //  @Ignore("cannot request total state: possibly too large")
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
            true);

    log.info("shardInfo {}", shardInfo);
  }

  @Test
  void testGetShardInfoExactFalse() throws Exception {
    log.info("Testing getShardInfo query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    ShardInfo shardInfo =
        client.getShardInfo(
            masterchainInfo.getLast(),
            masterchainInfo.getLast().getWorkchain(),
            masterchainInfo.getLast().shard,
            false);

    log.info("shardInfo {}", shardInfo);
  }

  @Test
  void testGetAllShardsInfo() throws Exception {
    log.info("Testing getAllShardsInfo query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    AllShardsInfo allShardInfo = client.getAllShardsInfo(masterchainInfo.getLast());

    log.info("allShardInfo {}", allShardInfo);
    log.info("allShardInfo.shardHashes {}", allShardInfo.getShardHashes());
  }

  @Test
  void testGetOneTransaction() throws Exception {
    log.info("Testing getOneTransaction query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    TransactionInfo transactionInfo =
        client.getOneTransaction(
            masterchainInfo.getLast(), Address.of(ELECTOR_ADDRESS), 35473445000001L);
    log.info("getOneTransaction {}", transactionInfo);
    log.info("getOneTransaction parsed {}", transactionInfo.getTransactionParsed());
  }

  @Test
  void testGetTransactionsByNone() throws Exception {
    log.info("Testing getTransactions query");
    assertTrue(client.isConnected(), "Client should be connected");

    TransactionList transactionList = client.getTransactions(Address.of(getAddress()), 0, null, 10);
    log.info("getTransactions {}", transactionList);

    for (Transaction tx : transactionList.getTransactionsParsed()) {
      log.info("tx {}", tx);
    }
  }

  @Test
  void testGetTransactionsByLtHash() throws Exception {
    log.info("Testing getTransactions query");
    assertTrue(client.isConnected(), "Client should be connected");

    TransactionList transactionList =
        client.getTransactions(
            Address.of(getAddress()),
            58178049000003L,
            Utils.hexToSignedBytes(
                "ce0c54c80f7a71da97b853f53d4850b84dd055cf4d412fb5a6a15f357f5350f4"),
            10);

    for (Transaction tx : transactionList.getTransactionsParsed()) {
      log.info("tx {}", tx);
    }
  }

  @Test
  void testLookupBlockMode2() throws Exception { // by LT
    log.info("Testing lookupBlock query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    BlockHeader blockHeader =
        client.lookupBlock(masterchainInfo.getLast().getBlockId(), 2, 35473445000001L, 0);
    log.info("blockHeader {}", blockHeader);
  }

  @Test
  void testLookupBlockMode4() throws Exception {
    log.info("Testing lookupBlock query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    BlockHeader blockHeader =
        client.lookupBlock(
            masterchainInfo.getLast().getBlockId(),
            4,
            0,
            Long.valueOf(Utils.now()).intValue() - 100);
    log.info("blockHeader {}", blockHeader);
  }

  @Test
  void testLookupBlockWithProofMode2() throws Exception { // by LT
    log.info("Testing lookupBlockWithProof query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    LookupBlockResult lookupBlockResult =
        client.lookupBlockWithProof(
            2,
            masterchainInfo.getLast().getBlockId(),
            masterchainInfo.getLast(),
            35473445000001L,
            0);

    log.info("lookupBlockResult {}", lookupBlockResult);
  }

  @Test
  void testLookupBlockWithProofMode4() throws Exception { // by UTIME
    log.info("Testing lookupBlockWithProof query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    LookupBlockResult lookupBlockResult =
        client.lookupBlockWithProof(
            4,
            masterchainInfo.getLast().getBlockId(),
            masterchainInfo.getLast(),
            0,
            Long.valueOf(Utils.now()).intValue() - 100);

    log.info("lookupBlockResult {}", lookupBlockResult);
  }

  /** fetch only accounts */
  @Test
  void testListBlockTransactionsMode0() throws Exception {
    log.info("Testing listBlockTransactions query");
    assertTrue(client.isConnected(), "Client should be connected");
    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    BlockTransactions blockTransactions =
        client.listBlockTransactions(masterchainInfo.getLast(), 0, 10, null);
    log.info("blockTransactions {}", blockTransactions);
    for (TransactionId txId : blockTransactions.getTransactionIds()) {
      log.info("txId {}", txId);
    }
  }

  /** fetch only accounts */
  @Test
  void testListBlockTransactionsMode1() throws Exception {
    log.info("Testing listBlockTransactions query");
    assertTrue(client.isConnected(), "Client should be connected");
    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    BlockTransactions blockTransactions =
        client.listBlockTransactions(masterchainInfo.getLast(), 1, 10, null);
    log.info("blockTransactions {}", blockTransactions);
    for (TransactionId txId : blockTransactions.getTransactionIds()) {
      log.info("txId {}", txId);
    }
  }

  /** fetch accounts, lt, hash */
  @Test
  void testListBlockTransactionsMode7() throws Exception {
    log.info("Testing listBlockTransactions query");
    assertTrue(client.isConnected(), "Client should be connected");
    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    BlockTransactions blockTransactions =
        client.listBlockTransactions(masterchainInfo.getLast(), 7, 10, null);
    log.info("blockTransactions {}", blockTransactions);
    for (TransactionId txId : blockTransactions.getTransactionIds()) {
      log.info("txId {}", txId);
    }
  }

  /** fetch metadata */
  @Test
  void testListBlockTransactionsMode256() throws Exception {
    log.info("Testing listBlockTransactions query");
    assertTrue(client.isConnected(), "Client should be connected");
    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    BlockTransactions blockTransactions =
        client.listBlockTransactions(masterchainInfo.getLast(), 256 + 7, 10, null);
    log.info("blockTransactions {}", blockTransactions);
    for (TransactionId txId : blockTransactions.getTransactionIds()) {
      log.info("txId {}", txId);
    }
  }

  @Test
  void testListBlockTransactionsAfter() throws Exception {
    log.info("Testing listBlockTransactions query");
    assertTrue(client.isConnected(), "Client should be connected");
    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    TransactionId3 after =
        TransactionId3.builder()
            .account(Address.of(getAddress()).hashPart)
            .lt(35473445000001L)
            .build();

    BlockTransactions blockTransactions =
        client.listBlockTransactions(masterchainInfo.getLast(), 128 + 7, 10, after); // 7th bit
    log.info("blockTransactions {}", blockTransactions);
    for (TransactionId txId : blockTransactions.getTransactionIds()) {
      log.info("txId {}", txId);
    }
  }

  @Test
  void testListBlockTransactionsExtMode0() throws Exception {
    log.info("Testing listBlockTransactionsExt query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    BlockTransactionsExt blockTransactionsExt =
        client.listBlockTransactionsExt(masterchainInfo.getLast(), 0, 10, null, false, false);
    log.info("listBlockTransactionsExt {}", blockTransactionsExt);
    for (Transaction tx : blockTransactionsExt.getTransactionsParsed()) {
      log.info("transaction tl-b {}", tx);
    }
  }

  @Test
  void testListBlockTransactionsExt() throws Exception {
    log.info("Testing listBlockTransactionsExt query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    BlockTransactionsExt blockTransactionsExt =
        client.listBlockTransactionsExt(masterchainInfo.getLast(), 0, 10, null, false, false);
    log.info("listBlockTransactionsExt {}", blockTransactionsExt);
    for (Transaction tx : blockTransactionsExt.getTransactionsParsed()) {
      log.info("transaction tl-b {}", tx);
    }
  }

  @Test
  void testListBlockTransactionsExtAfter() throws Exception {
    log.info("Testing listBlockTransactionsExt query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    TransactionId3 afterTx =
        TransactionId3.builder().account(Address.of(getAddress()).hashPart).lt(0).build();
    BlockTransactionsExt blockTransactionsExt =
        client.listBlockTransactionsExt(masterchainInfo.getLast(), 128, 10, afterTx, false, false);
    log.info("listBlockTransactionsExt {}", blockTransactionsExt);
    for (Transaction tx : blockTransactionsExt.getTransactionsParsed()) {
      log.info("transaction tl-b {}", tx);
    }
  }

  @Test
  void testListBlockTransactionsExtReverseOrder() throws Exception {
    log.info("Testing listBlockTransactionsExt query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    BlockTransactionsExt blockTransactionsExt =
        client.listBlockTransactionsExt(masterchainInfo.getLast(), 0, 10, null, true, false);
    log.info("listBlockTransactionsExt {}", blockTransactionsExt);
    for (Transaction tx : blockTransactionsExt.getTransactionsParsed()) {
      log.info("transaction tl-b {}", tx);
    }
  }

  @Test
  void testListBlockTransactionsExtWantedProof() throws Exception {
    log.info("Testing listBlockTransactionsExt query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    BlockTransactionsExt blockTransactionsExt =
        client.listBlockTransactionsExt(masterchainInfo.getLast(), 0, 10, null, false, true);
    log.info("listBlockTransactionsExt {}", blockTransactionsExt);
    for (Transaction tx : blockTransactionsExt.getTransactionsParsed()) {
      log.info("transaction tl-b {}", tx);
    }
  }

  @Test
  void testRunSmcMethod() throws Exception {
    log.info("Testing runSmcMethod query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    RunMethodResult runMethodResult =
        client.runMethod(masterchainInfo.getLast(), 0, Address.of(getAddress()), 0, new byte[0]);

    log.info("runMethodResult {}", runMethodResult);
    // todo
  }

  @Test
  void testSendMessage() throws Exception {
    log.info("Testing sendMessage query");
    assertTrue(client.isConnected(), "Client should be connected");

    String bocMessage =
        "B5EE9C724101030100F10002CF88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A11900AF60938844B0DDEE0D7F5C6C6B55C2D7F661170E029B8978AACCD402F2FF03FDD08D94398DB0826DA42FA96A6CBA73D232370025BF544D3A954208C990600A000000010010200BAFF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED5400480000000082A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB111EE9AE";
    SendMsgStatus sendMsgStatus = client.sendMessage(Utils.hexToSignedBytes(bocMessage));

    log.info("sendMsgStatus {}", sendMsgStatus);
    // todo
  }

  @Test
  void testValidatorStatsMode0() throws Exception {
    log.info("Testing validatorStats query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    ValidatorStats validatorStats =
        client.getValidatorStats(masterchainInfo.getLast(), 0, 10, null, 0);

    log.info("validatorStats {}", validatorStats);
  }

  @Test
  void testValidatorStatsMode1() throws Exception {
    log.info("Testing validatorStats query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    ValidatorStats validatorStats =
        client.getValidatorStats(masterchainInfo.getLast(), 1, 10, null, 0);

    log.info("validatorStats {}", validatorStats);
    // todo
  }

  @Test
  void testValidatorStatsMode4() throws Exception {
    log.info("Testing validatorStats query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    ValidatorStats validatorStats =
        client.getValidatorStats(masterchainInfo.getLast(), 4, 10, null, 0);

    log.info("validatorStats {}", validatorStats);
    // todo
  }

  @Test
  void testGetShardBlockProof() throws Exception {
    log.info("Testing getShardBlockProof query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    ShardBlockProof shardBlockProof = client.getShardBlockProof(masterchainInfo.getLast());

    log.info("shardBlockProof {}", shardBlockProof);
    assertThat(shardBlockProof.getMasterchainId().getSeqno()).isGreaterThan(0);
  }

  @Test
  void testGetBlockProofMode0() throws Exception {
    log.info("Testing getBlockProof query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    PartialBlockProof partialBlockProof = client.getBlockProof(0, masterchainInfo.getLast(), null);

    log.info("partialBlockProof {}", partialBlockProof);
    assertThat(partialBlockProof.getFrom().getSeqno()).isGreaterThan(0);
    // todo BlockLink
  }

  @Test
  void testGetBlockProofMode1() throws Exception {
    log.info("Testing getBlockProof query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo knownBlock = client.getMasterchainInfo();
    MasterchainInfo targetBlock = client.getMasterchainInfo();
    PartialBlockProof partialBlockProof =
        client.getBlockProof(0, knownBlock.getLast(), targetBlock.getLast());

    log.info("partialBlockProof {}", partialBlockProof);
    assertThat(partialBlockProof.getFrom().getSeqno()).isGreaterThan(0);
  }

  @Test
  void testGetDispatchQueueInfoMode0() throws Exception {
    log.info("Testing testDispatchQueueInfo query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    DispatchQueueInfo dispatchQueueInfo =
        client.getDispatchQueueInfo(masterchainInfo.getLast(), 0, null, 20, true);
    log.info("dispatchQueueInfo {}", dispatchQueueInfo);
  }

  @Test
  void testGetDispatchQueueInfoMode1() throws Exception { // with proof
    log.info("Testing testDispatchQueueInfo query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    DispatchQueueInfo dispatchQueueInfo =
        client.getDispatchQueueInfo(masterchainInfo.getLast(), 1, null, 20, true);
    log.info("dispatchQueueInfo {}", dispatchQueueInfo);
  }

  @Test
  void testGetDispatchQueueInfoMode2() throws Exception { // after address
    log.info("Testing testDispatchQueueInfo query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    DispatchQueueInfo dispatchQueueInfo =
        client.getDispatchQueueInfo(
            masterchainInfo.getLast(), 2, Address.of(getAddress()), 20, false);
    log.info("dispatchQueueInfo {}", dispatchQueueInfo);
  }

  @Test
  void testGetDispatchQueueInfoMode12() throws Exception { // after address
    log.info("Testing testDispatchQueueInfo query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    DispatchQueueInfo dispatchQueueInfo =
        client.getDispatchQueueInfo(
            masterchainInfo.getLast(), 1 + 2, Address.of(getAddress()), 20, false);
    log.info("dispatchQueueInfo {}", dispatchQueueInfo);
  }

  @Test
  void testGetDispatchQueueMessagesMode0() throws Exception {
    log.info("Testing testDispatchQueueMessages query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    DispatchQueueMessages dispatchQueueMessages =
        client.getDispatchQueueMessages(
            masterchainInfo.getLast(),
            0,
            Address.of(getAddress()),
            35473445000001L,
            10,
            false,
            false,
            false);
    log.info("dispatchQueueMessages {}", dispatchQueueMessages);
  }

  @Test
  void testGetDispatchQueueMessagesMode1() throws Exception { // with proof
    log.info("Testing testDispatchQueueMessages query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    DispatchQueueMessages dispatchQueueMessages =
        client.getDispatchQueueMessages(
            masterchainInfo.getLast(),
            1,
            Address.of(getAddress()),
            35473445000001L,
            10,
            false,
            false,
            false);
    log.info("dispatchQueueMessages {}", dispatchQueueMessages);
  }

  @Test
  void testGetDispatchQueueMessagesMode2() throws Exception { // one account
    log.info("Testing testDispatchQueueMessages query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    DispatchQueueMessages dispatchQueueMessages =
        client.getDispatchQueueMessages(
            masterchainInfo.getLast(),
            2,
            Address.of(getAddress()),
            35473445000001L,
            10,
            false,
            false,
            false);
    log.info("dispatchQueueMessages {}", dispatchQueueMessages);
  }

  @Test
  void testGetDispatchQueueMessagesMode4() throws Exception { // boc with messages
    log.info("Testing testDispatchQueueMessages query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    DispatchQueueMessages dispatchQueueMessages =
        client.getDispatchQueueMessages(
            masterchainInfo.getLast(),
            4,
            Address.of(getAddress()),
            35473445000001L,
            10,
            false,
            false,
            false);
    log.info("dispatchQueueMessages {}", dispatchQueueMessages);
  }

  @Test
  void testGetLibraries() throws Exception {
    log.info("Testing testGetLibraries query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    LibraryResult libraryResult = client.getLibraries(List.of(new byte[32]));
    log.info("libraryResult {}", libraryResult);
  }

  @Test
  void testGetLibrariesWithProofMode0() throws Exception {
    log.info("Testing testGetLibrariesWithProof query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    LibraryResultWithProof librariesWithProof =
        client.getLibrariesWithProof(masterchainInfo.getLast(), 0, List.of(new byte[32]));
    log.info("librariesWithProof {}", librariesWithProof);
  }

  @Test
  void testGetLibrariesWithProofMode1() throws Exception { // no mode used
    log.info("Testing testGetLibrariesWithProof query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    LibraryResultWithProof librariesWithProof =
        client.getLibrariesWithProof(masterchainInfo.getLast(), 1, List.of(new byte[32]));
    log.info("librariesWithProof {}", librariesWithProof);
  }

  @Test
  void testGetOutMsgQueueSizesQueryMode0() throws Exception { // no wc no shard are used
    log.info("Testing testGetOutMsgQueueSizesQuery query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    OutMsgQueueSizes outMsgQueueSizes = client.getOutMsgQueueSizesQuery(0, 0, 0);
    log.info("outMsgQueueSizes {}", outMsgQueueSizes);
    for (OutMsgQueueSize outMsg : outMsgQueueSizes.getShards()) {
      log.info("outMsg {}", outMsg);
    }
  }

  @Test
  void testGetOutMsgQueueSizesQueryMode1() throws Exception { // with wc and shard
    log.info("Testing testGetOutMsgQueueSizesQuery query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    OutMsgQueueSizes outMsgQueueSizes =
        client.getOutMsgQueueSizesQuery(1, -1, -9223372036854775808L);
    log.info("outMsgQueueSizes {}", outMsgQueueSizes);
    for (OutMsgQueueSize outMsg : outMsgQueueSizes.getShards()) {
      log.info("outMsg {}", outMsg);
    }
  }

  @Test
  void testGetBlockOutMsgQueueSizeMode0() throws Exception { // without proof
    log.info("Testing testGetBlockOutMsgQueueSize query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    BlockOutMsgQueueSize blockOutMsgQueueSize =
        client.getBlockOutMsgQueueSize(masterchainInfo.getLast(), 0, false);
    log.info("outMsgQueueSizes {}", blockOutMsgQueueSize);
  }

  @Test
  void testGetBlockOutMsgQueueSizeMode1() throws Exception { // with proof
    log.info("Testing testGetBlockOutMsgQueueSize query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    log.info("masterchainInfo {}", masterchainInfo.getLast());
    BlockOutMsgQueueSize blockOutMsgQueueSize =
        client.getBlockOutMsgQueueSize(masterchainInfo.getLast(), 1, true);
    log.info("outMsgQueueSizes {}", blockOutMsgQueueSize);
  }
}
