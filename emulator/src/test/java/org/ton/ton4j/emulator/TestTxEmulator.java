package org.ton.ton4j.emulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.iwebpp.crypto.TweetNaclFast;
import com.sun.jna.Native;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.TonHashMapE;
import org.ton.ton4j.emulator.tx.TxEmulator;
import org.ton.ton4j.emulator.tx.TxEmulatorI;
import org.ton.ton4j.emulator.tx.TxVerbosityLevel;
import org.ton.ton4j.fift.FiftRunner;
import org.ton.ton4j.func.FuncRunner;
import org.ton.ton4j.liteclient.LiteClient;
import org.ton.ton4j.smartcontract.SmartContractCompiler;
import org.ton.ton4j.smartcontract.types.Destination;
import org.ton.ton4j.smartcontract.types.WalletV5Config;
import org.ton.ton4j.smartcontract.wallet.v5.WalletV5;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tolk.TolkRunner;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.SmcLibraryEntry;
import org.ton.ton4j.tonlib.types.SmcLibraryResult;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestTxEmulator {

  static TxEmulator txEmulator;
  static Tonlib tonlib;
  static Cell config;
  static LiteClient liteClient;

  static Account testAccount;

  static String emulatorPath = Utils.getEmulatorGithubUrl();
  static String tonlibPath = Utils.getTonlibGithubUrl();
  static String funcPath = Utils.getFuncGithubUrl();
  static String fiftPath = Utils.getFiftGithubUrl();
  static String tolkPath = Utils.getTolkGithubUrl();
  static String liteClientPath = Utils.getLiteClientGithubUrl();

  @BeforeClass
  public static void setUpBeforeClass() {
    liteClient = LiteClient.builder().pathToLiteClientBinary(liteClientPath).build();

    tonlib =
        Tonlib.builder().pathToTonlibSharedLib(tonlibPath).testnet(true).ignoreCache(false).build();

    //    config = tonlib.getConfigAll(128);

    txEmulator =
        TxEmulator.builder()
            .pathToEmulatorSharedLib(emulatorPath)
            .configType(EmulatorConfig.TESTNET)
            .verbosityLevel(TxVerbosityLevel.TRUNCATED)
            .build();

    testAccount =
        Account.builder()
            .isNone(false)
            .address(
                MsgAddressIntStd.of(
                    "-1:0000000000000000000000000000000000000000000000000000000000000000"))
            .storageInfo(
                StorageInfo.builder()
                    .storageUsed(
                        StorageUsed.builder()
                            .cellsUsed(BigInteger.ZERO)
                            .bitsUsed(BigInteger.ZERO)
                            .build())
                    .storageExtraInfo(StorageExtraNone.builder().build())
                    .lastPaid(System.currentTimeMillis() / 1000)
                    .duePayment(Utils.toNano(2))
                    .build())
            .accountStorage(
                AccountStorage.builder()
                    .balance(
                        CurrencyCollection.builder()
                            .coins(Utils.toNano(2)) // initial balance
                            .build())
                    .accountState(
                        AccountStateActive.builder()
                            .stateInit(
                                StateInit.builder()
                                    .code(
                                        CellBuilder.beginCell()
                                            .fromBoc(
                                                "b5ee9c7241010101004e000098ff0020dd2082014c97ba9730ed44d0d70b1fe0a4f260810200d71820d70b1fed44d0d31fd3ffd15112baf2a122f901541044f910f2a2f80001d31f31d307d4d101fb00a4c8cb1fcbffc9ed5470102286")
                                            .endCell())
                                    // .data(CellBuilder.beginCell().storeBit(true).endCell())
                                    .build())
                            .build())
                    .accountStatus("ACTIVE")
                    .build())
            .build();
  }

  @Test
  public void testInitTxEmulator() throws IOException {

    String configAllTestnet =
        IOUtils.toString(
            Objects.requireNonNull(
                TestTxEmulator.class.getResourceAsStream("/config-all-testnet.txt")),
            StandardCharsets.UTF_8);

    TxEmulatorI txEmulatorI = Native.load("emulator.dll", TxEmulatorI.class);
    long emulator = txEmulatorI.transaction_emulator_create(configAllTestnet, 2);
    assertNotEquals(0, emulator);
  }

  @Test
  public void testSetVerbosityLevel() {
    txEmulator.setVerbosityLevel(4);
  }

  @Test
  public void testTxEmulatorDownload() {
    TxEmulator txEmulator =
        TxEmulator.builder()
            .pathToEmulatorSharedLib(
                "https://github.com/ton-blockchain/ton/releases/download/v2024.12-1/libemulator.dll")
            .configType(EmulatorConfig.TESTNET)
            .verbosityLevel(TxVerbosityLevel.TRUNCATED)
            .build();
    txEmulator.setVerbosityLevel(4);
  }

  @Test
  public void testSetDebugEnabled() {
    assertTrue(txEmulator.setDebugEnabled(false));
  }

  @Test
  public void testCustomConfig() throws IOException {

    String configAllTestnet =
        IOUtils.toString(
            Objects.requireNonNull(
                TestTxEmulator.class.getResourceAsStream("/config-all-testnet.txt")),
            StandardCharsets.UTF_8);

    txEmulator =
        TxEmulator.builder()
            .configType(EmulatorConfig.CUSTOM)
            .customConfig(configAllTestnet)
            .verbosityLevel(TxVerbosityLevel.UNLIMITED)
            .build();
    txEmulator.setVerbosityLevel(4);
  }

  @Test
  public void testTxEmulatorIgnoreCheckSignature() {
    assertTrue(txEmulator.setIgnoreCheckSignature(true));
  }

  @Test
  public void testTxEmulatorSetLt() {
    assertTrue(txEmulator.setEmulatorLt(200000));
  }

  @Test
  public void testTxEmulatorSetRandSeed() {
    assertTrue(txEmulator.setRandSeed(Utils.sha256("ABC")));
  }

  @Test
  public void testTxEmulatorSetUnixTime() {
    assertTrue(txEmulator.setUnixTime(System.currentTimeMillis() / 1000));
  }

  @Test
  public void testTxEmulatorSetConfig() {
    assertTrue(txEmulator.setConfig(tonlib.getConfigAll(128).toBase64()));
  }

  @Test
  public void testTxEmulatorCreateDestroyConfig() {
    String configBase64 = tonlib.getConfigAll(128).toBase64();
    long config = txEmulator.createConfig(configBase64);
    txEmulator.destroyConfig(config);
  }

  @Test
  public void testTxEmulatorSetLibs() {
    Cell dictLibs = getLibs();

    log.info("txEmulator.setLibs() result {}", txEmulator.setLibs(dictLibs.toBase64()));
  }

  @Test
  public void testTxEmulatorEmulateTickTx() {
    ShardAccount shardAccount =
        ShardAccount.builder()
            .account(testAccount)
            .lastTransHash(BigInteger.valueOf(2))
            .lastTransLt(BigInteger.ZERO)
            .build();

    log.info("shardAccount: {}", shardAccount);
    String shardAccountBocBase64 = shardAccount.toCell().toBase64();
    log.info("shardAccountCellBocBase64: {}", shardAccountBocBase64);
    EmulateTransactionResult result =
        txEmulator.emulateTickTockTransaction(shardAccountBocBase64, false);
    log.info("result {}", result);
    assertThat(result.success).isTrue();
    log.info("vm log {}", result.getVm_log());
  }

  @Test
  public void testTxEmulatorEmulateTockTx() {
    ShardAccount shardAccount =
        ShardAccount.builder()
            .account(testAccount)
            .lastTransHash(BigInteger.valueOf(2))
            .lastTransLt(BigInteger.ZERO)
            .build();

    log.info("shardAccount: {}", shardAccount);
    String shardAccountBocBase64 = shardAccount.toCell().toBase64();
    log.info("shardAccountCellBocBase64: {}", shardAccountBocBase64);
    EmulateTransactionResult result =
        txEmulator.emulateTickTockTransaction(shardAccountBocBase64, true);
    log.info("result {}", result);
    assertThat(result.success).isTrue();
  }

  @Test
  public void testTxEmulatorEmulateTxWithEmptyAccount() {

    ShardAccount shardAccount =
        ShardAccount.builder()
            .account(Account.builder().isNone(true).build())
            .lastTransHash(BigInteger.ZERO)
            .lastTransLt(BigInteger.ZERO)
            .build();
    String shardAccountBocBase64 = shardAccount.toCell().toBase64();

    Message internalMsg =
        Message.builder()
            .info(
                InternalMessageInfo.builder()
                    .srcAddr(
                        MsgAddressIntStd.builder()
                            .workchainId((byte) 0)
                            .address(BigInteger.ZERO)
                            .build())
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId((byte) 0)
                            .address(BigInteger.ZERO)
                            .build())
                    .value(CurrencyCollection.builder().coins(Utils.toNano(1)).build())
                    .bounce(false)
                    .createdAt(0)
                    .build())
            .init(null)
            .body(null)
            .build();
    String internalMsgBocBase64 = internalMsg.toCell().toBase64();
    EmulateTransactionResult result =
        txEmulator.emulateTransaction(shardAccountBocBase64, internalMsgBocBase64);
    log.info("result {}", result);
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  public void testTxEmulatorEmulateTxWithAccount() {

    ShardAccount shardAccount =
        ShardAccount.builder()
            .account(testAccount)
            .lastTransHash(BigInteger.ZERO)
            .lastTransLt(BigInteger.ZERO)
            .build();
    String shardAccountBocBase64 = shardAccount.toCell().toBase64();

    Message internalMsg =
        Message.builder()
            .info(
                InternalMessageInfo.builder()
                    .srcAddr(
                        MsgAddressIntStd.builder()
                            .workchainId((byte) 0)
                            .address(BigInteger.ZERO)
                            .build())
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId((byte) 0)
                            .address(BigInteger.ZERO)
                            .build())
                    .value(CurrencyCollection.builder().coins(Utils.toNano(1)).build())
                    .bounce(false)
                    .createdAt(0)
                    .build())
            .init(null)
            .body(null)
            .build();
    String internalMsgBocBase64 = internalMsg.toCell().toBase64();
    EmulateTransactionResult result =
        txEmulator.emulateTransaction(shardAccountBocBase64, internalMsgBocBase64);
    log.info("result {}", result);
    assertThat(result.isSuccess()).isTrue();
    log.info("new shardAccount {}", result.getNewShardAccount());
    log.info("new transaction {}", result.getTransaction());
    log.info("new actions {}", result.getActions());
  }

  @Test
  public void testTxEmulatorWalletV5ExternalMsg() {

    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder()
            .contractPath("G:/smartcontracts/new-wallet-v5.fc")
            .funcRunner(FuncRunner.builder().funcExecutablePath(funcPath).build())
            .fiftRunner(FiftRunner.builder().fiftExecutablePath(fiftPath).build())
            .tolkRunner(TolkRunner.builder().tolkExecutablePath(tolkPath).build())
            .build();

    String codeCellHex = smcFunc.compile();
    Cell codeCell = CellBuilder.beginCell().fromBoc(codeCellHex).endCell();

    byte[] publicKey =
        Utils.hexToSignedBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV5 walletV5 =
        WalletV5.builder()
            .keyPair(keyPair)
            .isSigAuthAllowed(false)
            .initialSeqno(0)
            .walletId(42)
            .build();

    Cell dataCell = walletV5.createDataCell();

    log.info("codeCellHex {}", codeCellHex);
    log.info("dataCellHex {}", dataCell.toHex());

    StateInit walletV5StateInit = StateInit.builder().code(codeCell).data(dataCell).build();

    Address address = walletV5StateInit.getAddress();
    log.info("addressRaw {}", address.toRaw());
    log.info("addressBounceable {}", address.toBounceable());

    Account walletV5Account =
        Account.builder()
            .isNone(false)
            .address(MsgAddressIntStd.of(address))
            .storageInfo(
                StorageInfo.builder()
                    .storageUsed(
                        StorageUsed.builder()
                            .cellsUsed(BigInteger.ZERO)
                            .bitsUsed(BigInteger.ZERO)
                            .build())
                    .storageExtraInfo(StorageExtraNone.builder().build())
                    .lastPaid(System.currentTimeMillis() / 1000)
                    .duePayment(BigInteger.ZERO)
                    .build())
            .accountStorage(
                AccountStorage.builder()
                    .lastTransactionLt(BigInteger.ZERO)
                    .balance(
                        CurrencyCollection.builder()
                            .coins(Utils.toNano(5)) // initial balance
                            .build())
                    .accountState(AccountStateActive.builder().stateInit(walletV5StateInit).build())
                    .build())
            .build();

    ShardAccount shardAccount =
        ShardAccount.builder()
            .account(walletV5Account)
            .lastTransHash(BigInteger.ZERO)
            .lastTransLt(BigInteger.ZERO)
            .build();

    String shardAccountBocBase64 = shardAccount.toCell().toBase64();

    //    txEmulator.setDebugEnabled(true);

    String rawDummyDestinationAddress =
        "0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d";

    WalletV5Config walletV5Config =
        WalletV5Config.builder()
            .seqno(0)
            .walletId(42)
            .body(
                walletV5
                    .createBulkTransfer(
                        Collections.singletonList(
                            Destination.builder()
                                .bounce(false)
                                .address(rawDummyDestinationAddress)
                                .amount(Utils.toNano(1.1))
                                .build()))
                    .toCell())
            .build();

    Message extMsg = walletV5.prepareExternalMsg(walletV5Config);

    txEmulator.setDebugEnabled(false);

    EmulateTransactionResult result =
        txEmulator.emulateTransaction(shardAccountBocBase64, extMsg.toCell().toBase64());

    //    log.info("result sendExternalMessage[1]: "+ result);

    ShardAccount newShardAccount = result.getNewShardAccount();
    //    log.info("new ShardAccount "+ newShardAccount);

    TransactionDescription txDesc = result.getTransaction().getDescription();
    //    log.info("txDesc "+ txDesc);

    TransactionDescriptionOrdinary txDescOrd = (TransactionDescriptionOrdinary) txDesc;

    ComputePhaseVM computePhase = (ComputePhaseVM) txDescOrd.getComputePhase();
    assertThat(computePhase.isSuccess()).isTrue();

    ActionPhase actionPhase = txDescOrd.getActionPhase();
    assertThat(actionPhase.isSuccess()).isTrue();

    //    log.info("txDescOrd "+ txDescOrd);
    assertThat(txDescOrd.isAborted()).isFalse();

    // transfer one more time
    walletV5Config =
        WalletV5Config.builder()
            .seqno(1)
            .walletId(42)
            .body(
                walletV5
                    .createBulkTransfer(
                        Collections.singletonList(
                            Destination.builder()
                                .bounce(false)
                                .address(rawDummyDestinationAddress)
                                .amount(Utils.toNano(1.2))
                                .build()))
                    .toCell())
            .build();

    extMsg = walletV5.prepareExternalMsg(walletV5Config);

    result = txEmulator.emulateTransaction(result.getShard_account(), extMsg.toCell().toBase64());
    //    log.info("result sendExternalMessage[2], exitCode: "+ result);
    assertThat(result.success).isTrue();

    newShardAccount = result.getNewShardAccount();
    //    log.info("new ShardAccount "+ newShardAccount);

    txDesc = result.getTransaction().getDescription();
    //    log.info("txDesc "+ txDesc);

    txDescOrd = (TransactionDescriptionOrdinary) txDesc;

    computePhase = (ComputePhaseVM) txDescOrd.getComputePhase();
    assertThat(computePhase.isSuccess()).isTrue();

    actionPhase = txDescOrd.getActionPhase();
    assertThat(actionPhase.isSuccess()).isTrue();

    //    log.info("txDescOrd "+ txDescOrd);
    assertThat(txDescOrd.isAborted()).isFalse();

    assertThat(newShardAccount.getAccount().getAccountStorage().getBalance().getCoins())
        .isLessThan(Utils.toNano(3.2));
    assertThat(newShardAccount.getBalance()).isLessThan(Utils.toNano(3.2)); // same as above
  }

  @Test
  public void testTxEmulatorWalletV5ExternalMsgSimplified() {

    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder()
            .contractPath("G:/smartcontracts/new-wallet-v5.fc")
            .funcRunner(FuncRunner.builder().funcExecutablePath(funcPath).build())
            .fiftRunner(FiftRunner.builder().fiftExecutablePath(fiftPath).build())
            .tolkRunner(TolkRunner.builder().tolkExecutablePath(tolkPath).build())
            .build();

    Cell codeCell = smcFunc.compileToCell();
    log.info("codeCellHex {}", codeCell.toHex());

    byte[] publicKey =
        Utils.hexToSignedBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV5 walletV5 =
        WalletV5.builder()
            .keyPair(keyPair)
            .isSigAuthAllowed(false)
            .initialSeqno(0)
            .walletId(42)
            .build();

    Cell dataCell = walletV5.createDataCell();

    log.info("codeCellHex {}", codeCell.toHex());
    log.info("dataCellHex {}", dataCell.toHex());

    StateInit walletV5StateInit = StateInit.builder().code(codeCell).data(dataCell).build();

    Address address = walletV5StateInit.getAddress();
    log.info("addressRaw {}", address.toRaw());
    log.info("addressBounceable {}", address.toBounceable());

    String rawDummyDestinationAddress =
        "0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d";

    WalletV5Config walletV5Config =
        WalletV5Config.builder()
            .seqno(0)
            .walletId(42)
            .body(
                walletV5
                    .createBulkTransfer(
                        Collections.singletonList(
                            Destination.builder()
                                .bounce(false)
                                .address(rawDummyDestinationAddress)
                                .amount(Utils.toNano(1))
                                .build()))
                    .toCell())
            .build();

    Message extMsg = walletV5.prepareExternalMsg(walletV5Config);

    EmulateTransactionResult result =
        txEmulator.emulateTransaction(
            codeCell, dataCell, Utils.toNano(2), extMsg.toCell().toBase64());
  }

  @Test
  public void testTxEmulatorWalletV5InternalMsg() {

    String contractAbsolutePath =
        System.getProperty("user.dir")
            + "/../smartcontract/src/test/resources/contracts/wallets/new-wallet-v5.fc";

    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder()
            .contractPath(contractAbsolutePath)
            .funcRunner(FuncRunner.builder().funcExecutablePath(funcPath).build())
            .fiftRunner(FiftRunner.builder().fiftExecutablePath(fiftPath).build())
            .tolkRunner(TolkRunner.builder().tolkExecutablePath(tolkPath).build())
            .build();

    String codeCellHex = smcFunc.compile();
    Cell codeCell = CellBuilder.beginCell().fromBoc(codeCellHex).endCell();

    byte[] publicKey =
        Utils.hexToSignedBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV5 walletV5 =
        WalletV5.builder()
            .keyPair(keyPair)
            .isSigAuthAllowed(false)
            .initialSeqno(0)
            .walletId(42)
            .build();

    Cell dataCell = walletV5.createDataCell();

    log.info("codeCellHex {}", codeCellHex);
    log.info("dataCellHex {}", dataCell.toHex());

    Address address = StateInit.builder().code(codeCell).data(dataCell).build().getAddress();
    log.info("addressRaw {}", address.toRaw());
    log.info("addressBounceable {}", address.toBounceable());

    Account walletV5Account =
        Account.builder()
            .isNone(false)
            .address(MsgAddressIntStd.of(address))
            .storageInfo(
                StorageInfo.builder()
                    .storageUsed(
                        StorageUsed.builder()
                            .cellsUsed(BigInteger.ZERO)
                            .bitsUsed(BigInteger.ZERO)
                            .build())
                    .storageExtraInfo(StorageExtraNone.builder().build())
                    .lastPaid(System.currentTimeMillis() / 1000)
                    .duePayment(BigInteger.ZERO)
                    .build())
            .accountStorage(
                AccountStorage.builder()
                    .lastTransactionLt(BigInteger.ZERO)
                    .balance(
                        CurrencyCollection.builder()
                            .coins(Utils.toNano(5)) // initial balance
                            .build())
                    .accountState(
                        AccountStateActive.builder()
                            .stateInit(StateInit.builder().code(codeCell).data(dataCell).build())
                            .build())
                    .build())
            .build();

    ShardAccount shardAccount =
        ShardAccount.builder()
            .account(walletV5Account)
            .lastTransHash(BigInteger.ZERO)
            .lastTransLt(BigInteger.ZERO)
            .build();
    String shardAccountBocBase64 = shardAccount.toCell().toBase64();

    txEmulator.setDebugEnabled(true);

    String rawDummyDestinationAddress =
        "0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d";

    WalletV5Config walletV5Config =
        WalletV5Config.builder()
            .seqno(0)
            .walletId(42)
            .amount(Utils.toNano(0.1))
            .body(
                walletV5
                    .createBulkTransfer(
                        Collections.singletonList(
                            Destination.builder()
                                .bounce(false)
                                .address(rawDummyDestinationAddress)
                                .amount(Utils.toNano(1))
                                .build()))
                    .toCell())
            .build();

    Message intMsg = walletV5.prepareInternalMsg(walletV5Config);

    EmulateTransactionResult result =
        txEmulator.emulateTransaction(shardAccountBocBase64, intMsg.toCell().toBase64());

    log.info("result emulateTransaction:  {}", result);
    assertThat(result.isSuccess()).isTrue();

    ShardAccount newShardAccount = result.getNewShardAccount();
    log.info("new ShardAccount {}", newShardAccount);

    TransactionDescription txDesc = result.getTransaction().getDescription();
    log.info("txDesc {}", txDesc);

    TransactionDescriptionOrdinary txDescOrd = (TransactionDescriptionOrdinary) txDesc;

    ComputePhaseVM computePhase = (ComputePhaseVM) txDescOrd.getComputePhase();
    assertThat(computePhase.isSuccess()).isTrue();

    ActionPhase actionPhase = txDescOrd.getActionPhase();
    assertThat(actionPhase.isSuccess()).isTrue();

    log.info("txDescOrd {}", txDescOrd);
    assertThat(txDescOrd.isAborted()).isFalse();
    // second transfer using new shard account

    walletV5Config =
        WalletV5Config.builder()
            .seqno(1)
            .walletId(42)
            .amount(Utils.toNano(0.1))
            .body(
                walletV5
                    .createBulkTransfer(
                        Collections.singletonList(
                            Destination.builder()
                                .bounce(false)
                                .address(rawDummyDestinationAddress)
                                .amount(Utils.toNano(1))
                                .build()))
                    .toCell())
            .build();

    intMsg = walletV5.prepareInternalMsg(walletV5Config);

    result =
        txEmulator.emulateTransaction(
            newShardAccount.toCell().toBase64(), intMsg.toCell().toBase64());

    log.info("result emulateTransaction:  {}", result);
    assertThat(result.success).isTrue();

    newShardAccount = result.getNewShardAccount();
    log.info("new ShardAccount {}", newShardAccount);

    txDesc = result.getTransaction().getDescription();
    log.info("txDesc {}", txDesc);

    txDescOrd = (TransactionDescriptionOrdinary) txDesc;

    computePhase = (ComputePhaseVM) txDescOrd.getComputePhase();
    assertThat(computePhase.isSuccess()).isTrue();

    actionPhase = txDescOrd.getActionPhase();
    assertThat(actionPhase.isSuccess()).isTrue();

    log.info("txDescOrd {}", txDescOrd);
    assertThat(txDescOrd.isAborted()).isFalse();

    assertThat(newShardAccount.getAccount().getAccountStorage().getBalance().getCoins())
        .isLessThan(Utils.toNano(3.2));
    assertThat(newShardAccount.getBalance()).isLessThan(Utils.toNano(3.2)); // same as above
  }

  private static Cell getLibs() {
    SmcLibraryResult result =
        tonlib.getLibraries(
            Collections.singletonList("wkUmK4wrzl6fzSPKM04dVfqW1M5pqigX3tcXzvy6P3M="));
    log.info("result: {}", result);

    TonHashMapE x = new TonHashMapE(256);

    for (SmcLibraryEntry l : result.getResult()) {
      String cellLibBoc = l.getData();
      Cell lib = Cell.fromBocBase64(cellLibBoc);
      log.info("cell lib {}", lib.toHex());
      x.elements.put(1L, lib);
    }

    Cell dictLibs =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
            v -> CellBuilder.beginCell().storeRef((Cell) v).endCell());
    return dictLibs;
  }
}
