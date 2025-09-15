package org.ton.ton4j.adnl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.globalconfig.TonGlobalConfig;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tl.liteserver.responses.*;
import org.ton.ton4j.tl.liteserver.responses.AccountState;
import org.ton.ton4j.tl.liteserver.responses.AllShardsInfo;
import org.ton.ton4j.tl.liteserver.responses.BlockData;
import org.ton.ton4j.tl.liteserver.responses.BlockHeader;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tlb.print.TransactionPrintInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class AdnlLiteClientTest {

  public static final String TESTNET_ADDRESS = "0QAyni3YDAhs7c-7imWvPyEbMEeVPMX8eWDLQ5GUe-B-Bl9Z";
  public static final String MAINNET_ADDRESS = "EQCRGnccIFznQqxm_oBm8PHz95iOe89Oe6hRAhSlAaMctuo6";
  public static final String MAINNET_V5_ADDRESS =
      "UQCHYR_fbDjjr1dtyMmgBbH3HSBAgSNwHdOZvAbgkNOV2n2D";
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
    client = AdnlLiteClient.builder().globalConfig(tonGlobalConfig).build();
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
  void testMasterchainInfoWithLiteServerContainerOnMyLocalTonDocker() throws Exception {
    TonGlobalConfig tonGlobalConfig =
        TonGlobalConfig.loadFromUrl(
            "http://localhost:8000/lite-server-localhost.global.config.json");

    AdnlLiteClient client = AdnlLiteClient.builder().globalConfig(tonGlobalConfig).build();

    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info, "Masterchain info should not be null");
    log.info("Masterchain info: {}", info);
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
  void testGetBalance() {
    log.info("Testing getBalance");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info(
        "account balance: {} ", Utils.formatNanoValue(client.getBalance(Address.of(getAddress()))));
  }

  @Test
  void testPrintTransactions() {
    log.info("Testing print transactions");
    assertTrue(client.isConnected(), "Client should be connected");

    client.printAccountTransactions(Address.of(getAddress()), true);
  }

  @Test
  void testPrintMessages() {
    log.info("Testing print messages");
    assertTrue(client.isConnected(), "Client should be connected");

    client.printAccountMessages(Address.of(getAddress()), 20);
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
  void testGetAccountStateNoStateTipo() throws Exception {
    log.info("Testing getAccountState");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info, "Masterchain info should not be null");
    assertNotNull(info.getLast(), "Last block should not be null");

    AccountState accountState =
        client.getAccountState(info.getLast(), Address.of(MAINNET_V5_ADDRESS));
    log.info("accountState: {} ", accountState);
    log.info("Last block seqno: {} ", accountState.getId().getSeqno());
    log.info("shard block seqno: {} ", accountState.getShardblk().getSeqno());
    assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");
    log.info("accountObject: {} ", accountState.getAccount());
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

    long timestamp = System.currentTimeMillis();
    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    BlockData blockData = client.getBlock(masterchainInfo.getLast());
    log.info("elapsed time: {} ms", System.currentTimeMillis() - timestamp); // ~450ms
    log.info("getBlock {}", blockData);
    log.info("Block  {}", blockData.getBlock());
    assertThat(blockData.getId().getSeqno()).isGreaterThan(0);
    assertThat(blockData.getBlock()).isNotNull();
  }

  @Test
  void testGetBlockFromMyLocalTon() throws Exception {

    TonGlobalConfig tonGlobalConfig =
        TonGlobalConfig.loadFromUrl("http://localhost:8000/localhost.global.config.json");

    AdnlLiteClient client = AdnlLiteClient.builder().globalConfig(tonGlobalConfig).build();

    assertTrue(client.isConnected(), "Client should be connected");

    long timestamp = System.currentTimeMillis();
    MasterchainInfo masterchainInfo = client.getMasterchainInfo();
    BlockData blockData = client.getBlock(masterchainInfo.getLast());
    log.info(
        "elapsed time: {} ms",
        System.currentTimeMillis() - timestamp); // ~20ms, ~20x faster than from a remote server
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
    //    log.info("configAll {}", configInfo);
    assertThat(configInfo.getId().getSeqno()).isGreaterThan(0);
    log.info("configParsed {}", configInfo.getConfigParams());
    //    Cell c = (Cell)
    // configInfo.getConfigParams().getConfig().elements.get(BigInteger.valueOf(32));
    //    log.info("cell {}", ValidatorSet.deserialize(CellSlice.beginParse(c)));
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
    log.info("configParsed {}", configInfo.getConfigParams());
  }

  @Test
  void testGetConfigParam0() {
    log.info("Testing testConfigParam0 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams0 {}", client.getConfigParam0());
  }

  @Test
  void testGetConfigParam1() {
    log.info("Testing testConfigParam1 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams1 {}", client.getConfigParam1());
  }

  @Test
  void testGetConfigParam2() {
    log.info("Testing testConfigParam2 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams2 {}", client.getConfigParam2());
  }

  @Test
  void testGetConfigParam3() {
    log.info("Testing testConfigParam3 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams3 {}", client.getConfigParam3());
  }

  @Test
  void testGetConfigParam4() {
    log.info("Testing testConfigParam4 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams4 {}", client.getConfigParam4());
  }

  @Test
  void testGetConfigParam5() {
    log.info("Testing testConfigParam5 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams5 {}", client.getConfigParam5());
  }

  @Test
  void testGetConfigParam6() {
    log.info("Testing testConfigParam6 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams6 {}", client.getConfigParam6());
    // todo
  }

  @Test
  void testGetConfigParam8() {
    log.info("Testing testConfigParam8 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams8 {}", client.getConfigParam8());
  }

  @Test
  void testGetConfigParam9() {
    log.info("Testing testConfigParam9 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams9 {}", client.getConfigParam9());
  }

  @Test
  void testGetConfigParam10() {
    log.info("Testing testConfigParam10 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams10 {}", client.getConfigParam10());
  }

  @Test
  void testGetConfigParam11() {
    log.info("Testing testConfigParam11 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams11 {}", client.getConfigParam11());
  }

  @Test
  void testGetConfigParam12() {
    log.info("Testing testConfigParam12 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams12 {}", client.getConfigParam12());
  }

  @Test
  void testGetConfigParam13() {
    log.info("Testing testConfigParam13 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams13 {}", client.getConfigParam13());
  }

  @Test
  void testGetConfigParam14() {
    log.info("Testing testConfigParam14 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams14 {}", client.getConfigParam14());
  }

  @Test
  void testGetConfigParam15() {
    log.info("Testing testConfigParam15 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams15 {}", client.getConfigParam15());
  }

  @Test
  void testGetConfigParam16() {
    log.info("Testing testConfigParam16 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams16 {}", client.getConfigParam16());
  }

  @Test
  void testGetConfigParam17() {
    log.info("Testing testConfigParam17 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams17 {}", client.getConfigParam17());
  }

  @Test
  void testGetConfigParam18() {
    log.info("Testing testConfigParam18 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams18 {}", client.getConfigParam18());
  }

  @Test
  void testGetConfigParam20() {
    log.info("Testing testConfigParam20 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams20 {}", client.getConfigParam20());
  }

  @Test
  void testGetConfigParam21() {
    log.info("Testing testConfigParam21 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams21 {}", client.getConfigParam21());
  }

  @Test
  void testGetConfigParam22() {
    log.info("Testing testConfigParam22 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams22 {}", client.getConfigParam22());
  }

  @Test
  void testGetConfigParam23() {
    log.info("Testing testConfigParam23 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams23 {}", client.getConfigParam23());
  }

  @Test
  void testGetConfigParam24() {
    log.info("Testing testConfigParam24 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams24 {}", client.getConfigParam24());
  }

  @Test
  void testGetConfigParam25() {
    log.info("Testing testConfigParam25 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams25 {}", client.getConfigParam25());
  }

  @Test
  void testGetConfigParam28() {
    log.info("Testing testConfigParam28 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams28 {}", client.getConfigParam28());
  }

  @Test
  void testGetConfigParam29() {
    log.info("Testing testConfigParam29 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams29 {}", client.getConfigParam29());
  }

  @Test
  void testGetConfigParam31() {
    log.info("Testing testConfigParam31 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams31 {}", client.getConfigParam31());
  }

  @Test
  void testGetConfigParam32() {
    log.info("Testing testConfigParam32 query");
    assertTrue(client.isConnected(), "Client should be connected");

    ConfigParams32 configParams32 = client.getConfigParam32();
    log.info("configParams32 {}", configParams32);
  }

  @Test
  void testGetConfigParam33() {
    log.info("Testing testConfigParam33 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams33 {}", client.getConfigParam33());
  }

  @Test
  void testGetConfigParam34() {
    log.info("Testing testConfigParam34 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams34 {}", client.getConfigParam34());
  }

  @Test
  void testGetConfigParam35() {
    log.info("Testing testConfigParam35 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams35 {}", client.getConfigParam35());
  }

  @Test
  void testGetConfigParam36() {
    log.info("Testing testConfigParam36 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams36 {}", client.getConfigParam36());
  }

  @Test
  void testGetConfigParam37() {
    log.info("Testing testConfigParam37 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams37 {}", client.getConfigParam37());
  }

  @Test
  void testGetConfigParam39() {
    log.info("Testing testConfigParam39 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams39 {}", client.getConfigParam39());
  }

  @Test
  void testGetConfigParam40() {
    log.info("Testing testConfigParam40 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams40 {}", client.getConfigParam40());
  }

  @Test
  void testGetConfigParam44() {
    log.info("Testing testConfigParam44 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams44 {}", client.getConfigParam44());
  }

  @Test
  void testGetConfigParam45() {
    log.info("Testing testConfigParam45 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams45 {}", client.getConfigParam45());
  }

  @Test
  void testGetConfigParam71() {
    log.info("Testing testConfigParam71 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams71 {}", client.getConfigParam71());
  }

  @Test
  void testGetConfigParam72() {
    log.info("Testing testConfigParam72 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams72 {}", client.getConfigParam72());
  }

  @Test
  void testGetConfigParam73() {
    log.info("Testing testConfigParam73 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams73 {}", client.getConfigParam73());
  }

  @Test
  void testGetConfigParam79() {
    log.info("Testing testConfigParam79 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams79 {}", client.getConfigParam79());
  }

  @Test
  void testGetConfigParam81() {
    log.info("Testing testConfigParam81 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams81 {}", client.getConfigParam81());
  }

  @Test
  void testGetConfigParam82() throws Exception {
    log.info("Testing testConfigParam82 query");
    assertTrue(client.isConnected(), "Client should be connected");

    log.info("configParams82 {}", client.getConfigParam82());
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
    for (ShardDescr shardDescr : allShardInfo.getShards()) {
      log.info("shard {}", shardDescr);
    }
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
  void testLookupBlockMode1() throws Exception { // by LT
    log.info("Testing lookupBlock query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    BlockHeader blockHeader = client.lookupBlock(masterchainInfo.getLast().getBlockId(), 1, 0, 0);
    log.info("blockHeader {}", blockHeader);
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
    TransactionPrintInfo.printTxHeader();
    for (Transaction tx : blockTransactionsExt.getTransactionsParsed()) {
      TransactionPrintInfo.printTransactionInfo(tx);
      TransactionPrintInfo.printAllMessages(tx, true, true);
    }
    TransactionPrintInfo.printTxFooter();
  }

  @Test
  void testPrintBlockTransactionsMode0() throws Exception {
    log.info("Testing listBlockTransactionsExt query");
    assertTrue(client.isConnected(), "Client should be connected");
    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    client.printBlockTransactions(masterchainInfo.getLast(), 0, 10, null, false, true);
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
    log.info("Testing runSmcMethod seqno query");
    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo masterchainInfo = client.getMasterchainInfo();

    RunMethodResult runMethodResult =
        client.runMethod(
            masterchainInfo.getLast(),
            4,
            Address.of(getAddress()),
            Utils.calculateMethodId("seqno"),
            new byte[0]);

    VmStack vmStack =
        VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(runMethodResult.result)));
    log.info("vmStack {}", vmStack);

    log.info("runMethodResult {}", runMethodResult);
  }

  @Test
  void testRunSmcMethodParticipantsList() throws Exception {
    log.info("Testing runSmcMethod participant_list query");
    assertTrue(client.isConnected(), "Client should be connected");

    RunMethodResult runMethodResult =
        client.runMethod(Address.of(ELECTOR_ADDRESS), "participant_list", new byte[0]);

    VmStack vmStack =
        VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(runMethodResult.result)));
    log.info("vmStack {}", vmStack);

    log.info("runMethodResult {}", runMethodResult);
  }

  @Test
  void testRunSmcMethodParticipantsListMethod() {
    log.info("Testing runSmcMethod participant_list query");
    assertTrue(client.isConnected(), "Client should be connected");

    List<Participant> participants = client.getElectionParticipants();

    log.info("runMethodResult {}", participants);
  }

  @Test
  void testRunSmcMethodSeqno() {
    log.info("Testing runSmcMethod seqno query");
    assertTrue(client.isConnected(), "Client should be connected");

    RunMethodResult runMethodResult = client.runMethod(Address.of(MAINNET_V5_ADDRESS), "seqno");

    VmStack vmStack =
        VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(runMethodResult.result)));
    log.info("vmStack {}", vmStack);

    log.info("runMethodResult {}", runMethodResult);
  }

  @Test
  void testRunSmcMethodSeqnoShort() throws Exception {
    log.info("Testing runSmcMethod seqno query");
    assertTrue(client.isConnected(), "Client should be connected");

    long seqno = client.getSeqno(Address.of(MAINNET_V5_ADDRESS));
    log.info("seqno {}", seqno);
  }

  /** wrong method name, exit code 11 */
  @Test
  void testRunSmcMethodPublicKey() {
    log.info("Testing runSmcMethod public_key query");
    assertTrue(client.isConnected(), "Client should be connected");

    RunMethodResult runMethodResult =
        client.runMethod(Address.of(MAINNET_V5_ADDRESS), "public_key");

    VmStack vmStack =
        VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(runMethodResult.result)));
    log.info("vmStack {}", vmStack);
    log.info("runMethodResult {}", runMethodResult);
    assertThat(runMethodResult.getExitCode()).isEqualTo(11);
  }

  @Test
  void testRunSmcMethodPublicKeyOk() {
    log.info("Testing runSmcMethod get_public_key query");
    assertTrue(client.isConnected(), "Client should be connected");

    RunMethodResult runMethodResult =
        client.runMethod(Address.of(MAINNET_V5_ADDRESS), "get_public_key");

    VmStack vmStack =
        VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(runMethodResult.result)));
    log.info("vmStack {}", vmStack);
    log.info("runMethodResult {}", runMethodResult);
  }

  @Test
  void testRunSmcMethodPublicKeyOkShort() {
    log.info("Testing runSmcMethod get_public_key query");
    assertTrue(client.isConnected(), "Client should be connected");

    BigInteger pubKey = client.getPublicKey(Address.of(MAINNET_V5_ADDRESS));
    log.info("pubKey {}", Utils.bytesToHex(Utils.to32ByteArray(pubKey)));
  }

  @Test
  void testRunSmcMethodGetSubWalletId() {
    log.info("Testing runSmcMethod get_subwallet_id query");
    assertTrue(client.isConnected(), "Client should be connected");

    long subWalletId = client.getSubWalletId(Address.of(MAINNET_V5_ADDRESS));
    log.info("subWalletId {}", subWalletId);
  }

  @Test
  void testRunSmcMethodWithParam() {
    log.info("Testing runSmcMethod compute_returned_stake address query");
    assertTrue(client.isConnected(), "Client should be connected");

    long subWalletId =
        client.computeReturnedStake(Address.of("Uf8KrqWGw1CTcUHRgqZE57aKBeSOK0iuxduwtlTHusmD5PWf"));
    log.info("returnedStake {}", subWalletId);
  }

  @Test
  void testRunSmcMethodWithParams() {
    log.info("Testing runSmcMethod compute_returned_stake address query");
    assertTrue(client.isConnected(), "Client should be connected");

    RunMethodResult runMethodResult =
        client.runMethod(
            Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333"),
            "compute_returned_stake",
            VmStackValueInt.builder()
                .value(
                    Address.of("Uf8KrqWGw1CTcUHRgqZE57aKBeSOK0iuxduwtlTHusmD5PWf").toBigInteger())
                .build());
    log.info("runMethodResult {}", runMethodResult);
    VmStack vmStackResult =
        VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(runMethodResult.result)));
    log.info("vmStackResult: " + vmStackResult);
    long stake =
        VmStackValueTinyInt.deserialize(
                CellSlice.beginParse(vmStackResult.getStack().getTos().get(0).toCell()))
            .getValue()
            .longValue();
    log.info("returnedStake {}", stake);
  }

  @Test
  void testSendMessage() {
    log.info("Testing sendMessage query");
    assertTrue(client.isConnected(), "Client should be connected");

    String bocMessage =
        "B5EE9C724101030100F10002CF88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A11900AF60938844B0DDEE0D7F5C6C6B55C2D7F661170E029B8978AACCD402F2FF03FDD08D94398DB0826DA42FA96A6CBA73D232370025BF544D3A954208C990600A000000010010200BAFF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED5400480000000082A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB111EE9AE";
    SendMsgStatus sendMsgStatus =
        client.sendMessage(Message.deserialize(CellSlice.beginParse(Cell.fromBoc(bocMessage))));

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
        client.getValidatorStats(masterchainInfo.getLast(), 4, 10, new byte[32], 0);

    log.info("validatorStats {}", validatorStats);
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

  @Test
  public void testAccountBalanceAdnl() throws Exception {
    Address beneficiaryAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");
    log.info("beneficiaryAddress: {}", beneficiaryAddress.toBounceable());

    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();
    log.info("balance {}", adnlLiteClient.getBalance(beneficiaryAddress));
  }

  @Test
  public void testAccountBalanceAdnlOk() throws Exception {
    Address beneficiaryAddress = Address.of("0QAyni3YDAhs7c-7imWvPyEbMEeVPMX8eWDLQ5GUe-B-Bl9Z");
    log.info("beneficiaryAddress: {}", beneficiaryAddress.toBounceable());

    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();
    log.info("balance {}", adnlLiteClient.getBalance(beneficiaryAddress));
  }

  @Test
  public void testUpdateInitBlock() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();
    adnlLiteClient.persistGlobalConfig();
    log.info(adnlLiteClient.getPersistedGlobalConfigPath());
    adnlLiteClient.updateInitBlock();
  }

  @Test
  public void testUpdateInitBlockByPath() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();
    adnlLiteClient.updateInitBlock("path/to/global.config.json");
  }

  //  @Test
  //  public void testAccountBalanceTonlib() throws Exception {
  //    Address beneficiaryAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");
  //    log.info("beneficiaryAddress: {}", beneficiaryAddress.toBounceable());
  //
  //    log.info("balance {}", Utils.formatNanoValue(tonlib.getAccountBalance(beneficiaryAddress)));
  //  }
}
