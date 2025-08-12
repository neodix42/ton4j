package org.ton.ton4j.smartcontract.integrationtests;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.faucet.TestnetJettonFaucet;
import org.ton.ton4j.smartcontract.highload.HighloadWalletV3S;
import org.ton.ton4j.smartcontract.token.ft.JettonMinter;
import org.ton.ton4j.smartcontract.token.ft.JettonWallet;
import org.ton.ton4j.smartcontract.types.Destination;
import org.ton.ton4j.smartcontract.types.HighloadQueryId;
import org.ton.ton4j.smartcontract.types.HighloadV3Config;
import org.ton.ton4j.smartcontract.types.HighloadV3InternalMessageBody;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.toncenter.Network;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.VerbosityLevel;
import org.ton.ton4j.utils.Secp256k1KeyPair;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class TestHighloadWalletV3S extends CommonTest {

  @Test
  public void testBulkTransferSimplified_TwoDestinations() throws InterruptedException {

    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S contract =
        HighloadWalletV3S.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getPrivateKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            // .createdAt(createdAt) // default = now - 60 seconds
            // .timeOut(60) //default timeout = 5 minutes
            .build();

    SendResponse sendResponse = contract.deploy(config);
    log.info("extMessageInfo {}", sendResponse);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(45);
    log.info("deployed");

    int queryId = HighloadQueryId.fromSeqno(1).getQueryId();
    config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(queryId)
            .body(
                contract.createBulkTransfer(
                    Arrays.asList(
                        Destination.builder()
                            .address("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO")
                            .amount(Utils.toNano(0.12))
                            .body(
                                CellBuilder.beginCell()
                                    .storeUint(0, 32)
                                    .storeString("test-comment-1")
                                    .endCell())
                            .build(),
                        Destination.builder()
                            .address("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G")
                            .amount(Utils.toNano(0.11))
                            .comment("test-comment-2")
                            .build()),
                    BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
            .build();

    sendResponse = contract.send(config);
    log.info("extMessageInfo {}", sendResponse);
    assertThat(sendResponse.getCode()).isZero();
    log.info("sent 2 messages");

    String publicKey = contract.getPublicKey();
    log.info("public key {}", publicKey);

    long subWalletId = contract.getSubWalletId();
    log.info("subWalletId key {}", subWalletId);

    long lastCleanTime = contract.getLastCleanTime();
    log.info("lastCleanTime {}", lastCleanTime);

    long timeout = contract.getTimeout();
    log.info("timeout {}", timeout);

    boolean isProcessed = contract.isProcessed(queryId, true);
    log.info("isProcessed with clean {}", isProcessed);

    isProcessed = contract.isProcessed(queryId, false);
    log.info("isProcessed without clean {}", isProcessed);
  }

  @Test
  public void testBulkTransferSimplified_TwoDestinations_ExternalSigning()
      throws InterruptedException {

    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();
    byte[] pubKey = keyPair.getPublicKey();

    HighloadWalletV3S contract =
        HighloadWalletV3S.builder()
            .tonlib(tonlib)
            .publicKey(pubKey) // no private key is used
            .walletId(42)
            .build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(pubKey));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    Cell deployBody = contract.createDeployMessage(config);

    // sign deployBody without exposing private key and come back with a signature
    byte[] signedDeployBody =
        Utils.signDataSecp256k1(deployBody.hash(), keyPair.getPrivateKey(), pubKey).getSignature();

    SendResponse sendResponse = contract.deploy(config, signedDeployBody);
    log.info("extMessageInfo {}", sendResponse);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(45);
    log.info("deployed");

    int queryId = HighloadQueryId.fromSeqno(1).getQueryId();
    config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(queryId)
            .body(
                contract.createBulkTransfer(
                    Arrays.asList(
                        Destination.builder()
                            .address("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO")
                            .amount(Utils.toNano(0.12))
                            .body(
                                CellBuilder.beginCell()
                                    .storeUint(0, 32)
                                    .storeString("test-comment-1")
                                    .endCell())
                            .build(),
                        Destination.builder()
                            .address("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G")
                            .amount(Utils.toNano(0.11))
                            .comment("test-comment-2")
                            .build()),
                    BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
            .build();

    Cell transferBody = contract.createTransferMessage(config);

    // sign transferBody without exposing private key and come back with a signature
    byte[] signedTransferBody =
        Utils.signDataSecp256k1(
                transferBody.hash(), keyPair.getPrivateKey(), keyPair.getPublicKey())
            .getSignature();

    sendResponse = contract.send(config, signedTransferBody);
    log.info("extMessageInfo {}", sendResponse);
    assertThat(sendResponse.getCode()).isZero();
    log.info("sent 2 messages");

    String publicKey = contract.getPublicKey();
    log.info("public key {}", publicKey);

    long subWalletId = contract.getSubWalletId();
    log.info("subWalletId key {}", subWalletId);

    long lastCleanTime = contract.getLastCleanTime();
    log.info("lastCleanTime {}", lastCleanTime);

    long timeout = contract.getTimeout();
    log.info("timeout {}", timeout);

    boolean isProcessed = contract.isProcessed(queryId, true);
    log.info("isProcessed with clean {}", isProcessed);

    isProcessed = contract.isProcessed(queryId, false);
    log.info("isProcessed without clean {}", isProcessed);
  }

  /** Sends 1000 messages with values without comment/memo field */
  @Test
  public void testBulkTransferSimplified_1000() throws InterruptedException {

    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S contract =
        HighloadWalletV3S.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getPrivateKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(2));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    SendResponse sendResponse = contract.deploy(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(45);

    config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .body(
                contract.createBulkTransfer(
                    createDummyDestinations(1000),
                    BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();
    log.info("sent 1000 messages");
  }

  /** Sends 1000 messages with jetton values with comment */
  @Test
  public void testBulkJettonTransferSimplified_1000()
      throws InterruptedException, NoSuchAlgorithmException {

    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S contract =
        HighloadWalletV3S.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getPrivateKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(12));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    SendResponse sendResponse = contract.deploy(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(45);

    config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .body(
                contract.createBulkTransfer(
                    createDummyDestinations(300),
                    BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();
    log.info("sent 1000 messages");
  }

  @Test
  public void testSinglePayloadTransferWithCustomPrivateKeyOnExistingContract() {

    Secp256k1KeyPair keyPair =
        Utils.getSecp256k1FromPrivateKey(
            "dc49ff027f024955f3d2a4cd7d1e0ff9cfbc2fff57a2a3f69c2144478756e6d3");

    HighloadWalletV3S contract =
        HighloadWalletV3S.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getPrivateKey()));

    String publicKey = contract.getPublicKey();
    log.info("public key {}", publicKey);

    long subWalletId = contract.getSubWalletId();
    log.info("subWalletId key {}", subWalletId);

    int queryId1 = HighloadQueryId.fromSeqno(1).getQueryId();
    boolean isProcessed = contract.isProcessed(queryId1, false);
    log.info("isProcessed1 {}", isProcessed);

    int queryId2 = HighloadQueryId.fromSeqno(2).getQueryId();
    isProcessed = contract.isProcessed(queryId2, false);
    log.info("isProcessed2 {}", isProcessed);

    Address destAddress = Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G");

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(queryId2)
            .body(
                contract.createSingleTransfer(
                    destAddress,
                    Utils.toNano(0.02),
                    true,
                    null,
                    MsgUtils.createTextMessageBody("ton4j test")))
            .build();

    SendResponse sendResponse = contract.send(config);
    log.info("extMessageInfo {}", sendResponse);
    assertThat(sendResponse.getCode()).isZero();
    log.info("sent single message");

    publicKey = contract.getPublicKey();
    log.info("public key {}", publicKey);

    subWalletId = contract.getSubWalletId();
    log.info("subWalletId key {}", subWalletId);
  }

  @Test
  public void testSinglePayloadTransfer() throws InterruptedException {

    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S contract =
        HighloadWalletV3S.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getPrivateKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    Address destAddress = Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G");

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            //                .createdAt(createdAt) // default = now - 60 seconds
            // .timeOut(60) //default timeout = 5 minutes
            .build();

    SendResponse sendResponse = contract.deploy(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(45);

    config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .body(
                contract.createSingleTransfer(
                    destAddress,
                    Utils.toNano(0.02),
                    true,
                    null,
                    MsgUtils.createTextMessageBody("ton4j test")))
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();
    log.info("sent single message");
  }

  @Test
  public void testTransfer_3_DifferentRecipients()
      throws InterruptedException, NoSuchAlgorithmException {
    tonlib =
        Tonlib.builder()
            .testnet(true)
            .ignoreCache(false)
            .verbosityLevel(VerbosityLevel.DEBUG)
            .build();

    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S contract =
        HighloadWalletV3S.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getPrivateKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    SendResponse sendResponse = contract.deploy(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(30);

    int numberOfRecipients = 3;
    BigInteger amountToSendTotal = Utils.toNano(0.01 * numberOfRecipients);

    Cell nMessages = createDummyRecipients(numberOfRecipients, contract, null);

    Cell extMsgWith3Mgs = contract.createBulkTransfer(amountToSendTotal, nMessages);

    config =
        HighloadV3Config.builder()
            .walletId(42)
            .body(extMsgWith3Mgs)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();
    log.info("sent {} messages", numberOfRecipients);
  }

  @Test
  public void testTransfer_200_CustomCell() throws InterruptedException, NoSuchAlgorithmException {

    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S contract =
        HighloadWalletV3S.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getPrivateKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(3));

    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    //        long createdAt = Instant.now().getEpochSecond() - 60 * 5;

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    SendResponse sendResponse = contract.deploy(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(30);

    int numberOfRecipients = 200;
    BigInteger amountToSendTotal = Utils.toNano(0.01 * numberOfRecipients);

    Cell nMessages = createDummyRecipients(numberOfRecipients, contract, null);

    Cell extMsgWith200Mgs = contract.createBulkTransfer(amountToSendTotal, nMessages);

    config =
        HighloadV3Config.builder()
            .walletId(42)
            .body(extMsgWith200Mgs)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();
    log.info("sent {} messages", numberOfRecipients);
  }

  @Test
  public void testTransfer_200_CustomCell_AdnlLiteClient() throws Exception {

    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();
    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S contract =
        HighloadWalletV3S.builder()
            .adnlLiteClient(adnlLiteClient)
            .keyPair(keyPair)
            .walletId(42)
            .build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getPrivateKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(nonBounceableAddress), Utils.toNano(3));

    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    //        long createdAt = Instant.now().getEpochSecond() - 60 * 5;

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    SendResponse sendResponse = contract.deploy(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(30);

    int numberOfRecipients = 200;
    BigInteger amountToSendTotal = Utils.toNano(0.01 * numberOfRecipients);

    Cell nMessages = createDummyRecipients(numberOfRecipients, contract, null);

    Cell extMsgWith200Mgs = contract.createBulkTransfer(amountToSendTotal, nMessages);

    config =
        HighloadV3Config.builder()
            .walletId(42)
            .body(extMsgWith200Mgs)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();
    log.info("sent {} messages", numberOfRecipients);
  }

  @Test
  public void testTransfer_1000_CustomCell() throws InterruptedException, NoSuchAlgorithmException {

    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S contract =
        HighloadWalletV3S.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(12));

    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    SendResponse sendResponse = contract.deploy(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(30);

    Cell nMessages1 = createDummyRecipients(250, contract, null);
    Cell nMessages2 = createDummyRecipients(250, contract, nMessages1);
    Cell nMessages3 = createDummyRecipients(250, contract, nMessages2);
    Cell nMessages4 = createDummyRecipients(250, contract, nMessages3);
    Cell extMsgWith400Mgs = contract.createBulkTransfer(Utils.toNano(11), nMessages4);

    config =
        HighloadV3Config.builder()
            .walletId(42)
            .body(extMsgWith400Mgs)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();
    log.info("sent {} messages", 1000);
  }

  @Test
  public void testHighloadQueryId() {
    HighloadQueryId qid = new HighloadQueryId();
    assertThat(qid.getQueryId()).isEqualTo(0);
    qid = qid.getNext();
    assertThat(qid.getQueryId()).isEqualTo(1);
    for (int i = 0; i < 1022; i++) {
      qid = qid.getNext();
    }
    assertThat(qid.getQueryId()).isEqualTo(1024);
    assertThat(qid.toSeqno()).isEqualTo(1023);

    qid = HighloadQueryId.fromShiftAndBitNumber(8191, 1020);
    assertThat(qid.hasNext()).isTrue();
    qid = qid.getNext();
    assertThat(qid.hasNext()).isFalse();

    int nqid = qid.getQueryId();
    qid = HighloadQueryId.fromSeqno(qid.toSeqno());
    assertThat(nqid).isEqualTo(qid.getQueryId());
    qid = HighloadQueryId.fromQueryId(qid.getQueryId());
    assertThat(nqid).isEqualTo(qid.getQueryId());
  }

  Cell createDummyRecipients(int numRecipients, HighloadWalletV3S contract, Cell body)
      throws NoSuchAlgorithmException {
    List<OutAction> outActions = new ArrayList<>();
    for (int i = 0; i < numRecipients; i++) {
      Address destinationAddress = Address.of(Utils.generateRandomAddress(0));
      log.info("dest {} is {}", i, destinationAddress.toBounceable());
      OutAction outAction =
          ActionSendMsg.builder()
              .mode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS.getValue())
              .outMsg(
                  MessageRelaxed.builder()
                      .info(
                          InternalMessageInfoRelaxed.builder()
                              .bounce(false) // warning, for tests only
                              .dstAddr(
                                  MsgAddressIntStd.builder()
                                      .workchainId(destinationAddress.wc)
                                      .address(destinationAddress.toBigInteger())
                                      .build())
                              .value(CurrencyCollection.builder().coins(Utils.toNano(0.01)).build())
                              .build())
                      .build())
              .build();
      outActions.add(outAction);
    }

    if (nonNull(
        body)) { // one of those N messages can contain internal message with other X recipients
      OutAction outAction =
          ActionSendMsg.builder()
              .mode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS.getValue())
              .outMsg(
                  MessageRelaxed.builder()
                      .info(
                          InternalMessageInfoRelaxed.builder()
                              .dstAddr(
                                  MsgAddressIntStd.builder()
                                      .workchainId(contract.getAddress().wc)
                                      .address(contract.getAddress().toBigInteger())
                                      .build())
                              .value(CurrencyCollection.builder().coins(Utils.toNano(0.01)).build())
                              .build())
                      .body(body) // added other X messages
                      .build())
              .build();
      outActions.add(outAction);
    }

    return HighloadV3InternalMessageBody.builder()
        .queryId(BigInteger.ZERO)
        .actions(OutList.builder().actions(outActions).build())
        .build()
        .toCell();
  }

  @Test
  public void testBulkJettonTransfer() throws InterruptedException {

    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S myHighLoadWalletV3 =
        HighloadWalletV3S.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = myHighLoadWalletV3.getAddress().toNonBounceable();
    String bounceableAddress = myHighLoadWalletV3.getAddress().toBounceable();
    String rawAddress = myHighLoadWalletV3.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(myHighLoadWalletV3.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(myHighLoadWalletV3.getKeyPair().getPrivateKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    log.info(
        "new wallet {} toncoins balance: {}",
        myHighLoadWalletV3.getName(),
        Utils.formatNanoValue(balance));

    // top up new wallet with NEOJ using test-jetton-faucet-wallet
    balance =
        TestnetJettonFaucet.topUpContractWithNeoj(
            tonlib, Address.of(nonBounceableAddress), BigInteger.valueOf(100));
    log.info(
        "new wallet {} jetton balance: {}",
        myHighLoadWalletV3.getName(),
        Utils.formatJettonValue(balance, 2, 2));

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    SendResponse sendResponse = myHighLoadWalletV3.deploy(config);
    Assertions.assertThat(sendResponse.getCode()).isZero();

    myHighLoadWalletV3.waitForDeployment(60);

    String singleRandomAddress = Utils.generateRandomAddress(0);

    JettonMinter jettonMinterWallet =
        JettonMinter.builder()
            .tonlib(tonlib)
            .customAddress(Address.of("kQAN6TAGauShFKDQvZCwNb_EeTUIjQDwRZ9t6GOn4FBzfg9Y"))
            .build();

    JettonWallet myJettonHighLoadWallet =
        jettonMinterWallet.getJettonWallet(myHighLoadWalletV3.getAddress());

    config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .body(
                myHighLoadWalletV3.createBulkTransfer(
                    Collections.singletonList(
                        Destination.builder()
                            .address(myJettonHighLoadWallet.getAddress().toBounceable())
                            .amount(Utils.toNano(0.07))
                            .body(
                                JettonWallet.createTransferBody(
                                    0,
                                    BigInteger.valueOf(100),
                                    Address.of(singleRandomAddress), // recipient
                                    myJettonHighLoadWallet.getAddress(), // response address
                                    null, // custom payload
                                    BigInteger.ONE, // forward amount
                                    MsgUtils.createTextMessageBody("test sdk") // forward payload
                                    ))
                            .build()),
                    BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .build();

    sendResponse = myHighLoadWalletV3.send(config);
    Assertions.assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(60, "sending jettons...");

    BigInteger balanceOfDestinationWallet =
        tonlib.getAccountBalance(Address.of(singleRandomAddress));
    log.info("balanceOfDestinationWallet in nanocoins: {}", balanceOfDestinationWallet);

    JettonWallet randomJettonWallet =
        jettonMinterWallet.getJettonWallet(Address.of(singleRandomAddress));
    log.info("balanceOfDestinationWallet in jettons: {}", randomJettonWallet.getBalance());
  }

  List<Destination> createDummyDestinations(int count) {
    List<Destination> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String dstDummyAddress = Utils.generateRandomAddress(0);

      result.add(
          Destination.builder()
              .bounce(false)
              .address(dstDummyAddress)
              .amount(Utils.toNano(0.001))
              //                    .comment("comment-" + i)
              .build());
    }
    return result;
  }

  @Test
  public void testBulkJettonTransferAdnlLiteClient() throws Exception {

    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();
    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S myHighLoadWalletV3 =
        HighloadWalletV3S.builder()
            .adnlLiteClient(adnlLiteClient)
            .keyPair(keyPair)
            .walletId(42)
            .build();

    String nonBounceableAddress = myHighLoadWalletV3.getAddress().toNonBounceable();
    String bounceableAddress = myHighLoadWalletV3.getAddress().toBounceable();
    String rawAddress = myHighLoadWalletV3.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(myHighLoadWalletV3.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(myHighLoadWalletV3.getKeyPair().getPrivateKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(nonBounceableAddress), Utils.toNano(1));
    log.info(
        "new wallet {} toncoins balance: {}",
        myHighLoadWalletV3.getName(),
        Utils.formatNanoValue(balance));

    // top up new wallet with NEOJ using test-jetton-faucet-wallet

    balance =
        TestnetJettonFaucet.topUpContractWithNeoj(
            tonlib, Address.of(nonBounceableAddress), BigInteger.valueOf(100));
    balance =
        TestnetJettonFaucet.topUpContractWithNeoj(
            adnlLiteClient, Address.of(nonBounceableAddress), BigInteger.valueOf(100));
    log.info(
        "new wallet {} jetton balance: {}",
        myHighLoadWalletV3.getName(),
        Utils.formatJettonValue(balance, 2, 2));

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    SendResponse sendResponse = myHighLoadWalletV3.deploy(config);
    Assertions.assertThat(sendResponse.getCode()).isZero();

    myHighLoadWalletV3.waitForDeployment(60);

    String singleRandomAddress = Utils.generateRandomAddress(0);

    JettonMinter jettonMinterWallet =
        JettonMinter.builder()
            .adnlLiteClient(adnlLiteClient)
            .customAddress(Address.of("kQAN6TAGauShFKDQvZCwNb_EeTUIjQDwRZ9t6GOn4FBzfg9Y"))
            .build();

    JettonWallet myJettonHighLoadWallet =
        jettonMinterWallet.getJettonWallet(myHighLoadWalletV3.getAddress());

    config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .body(
                myHighLoadWalletV3.createBulkTransfer(
                    Collections.singletonList(
                        Destination.builder()
                            .address(myJettonHighLoadWallet.getAddress().toBounceable())
                            .amount(Utils.toNano(0.07))
                            .body(
                                JettonWallet.createTransferBody(
                                    0,
                                    BigInteger.valueOf(100),
                                    Address.of(singleRandomAddress), // recipient
                                    myJettonHighLoadWallet.getAddress(), // response address
                                    null, // custom payload
                                    BigInteger.ONE, // forward amount
                                    MsgUtils.createTextMessageBody("test sdk") // forward payload
                                    ))
                            .build()),
                    BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .build();

    sendResponse = myHighLoadWalletV3.send(config);
    Assertions.assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(60, "sending jettons...");

    BigInteger balanceOfDestinationWallet =
        adnlLiteClient.getBalance(Address.of(singleRandomAddress));
    log.info("balanceOfDestinationWallet in nanocoins: {}", balanceOfDestinationWallet);

    JettonWallet randomJettonWallet =
        jettonMinterWallet.getJettonWallet(Address.of(singleRandomAddress));
    log.info("balanceOfDestinationWallet in jettons: {}", randomJettonWallet.getBalance());
  }


  @Test
  public void testBulkJettonTransferTonCenterClient() throws Exception {
    TonCenter tonCenter =
            TonCenter.builder()
                    .apiKey(TESTNET_API_KEY)
                    .testnet()
                    .build();

    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S myHighLoadWalletV3 =
            HighloadWalletV3S.builder()
                    .tonCenterClient(tonCenter)
                    .keyPair(keyPair)
                    .walletId(42)
                    .build();

    String nonBounceableAddress = myHighLoadWalletV3.getAddress().toNonBounceable();
    String bounceableAddress = myHighLoadWalletV3.getAddress().toBounceable();
    String rawAddress = myHighLoadWalletV3.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(myHighLoadWalletV3.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(myHighLoadWalletV3.getKeyPair().getPrivateKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
            TestnetFaucet.topUpContract(
                    tonCenter, Address.of(nonBounceableAddress), Utils.toNano(1), true);
    log.info(
            "new wallet {} toncoins balance: {}",
            myHighLoadWalletV3.getName(),
            Utils.formatNanoValue(balance));

    // top up new wallet with NEOJ using test-jetton-faucet-wallet

    balance =
            TestnetJettonFaucet.topUpContractWithNeoj(
                    tonCenter, Address.of(nonBounceableAddress), BigInteger.valueOf(100), true);
    balance =
            TestnetJettonFaucet.topUpContractWithNeoj(
                    tonCenter, Address.of(nonBounceableAddress), BigInteger.valueOf(100), true);
    log.info(
            "new wallet {} jetton balance: {}",
            myHighLoadWalletV3.getName(),
            Utils.formatJettonValue(balance, 2, 2));

    HighloadV3Config config =
            HighloadV3Config.builder()
                    .walletId(42)
                    .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
                    .build();

    SendResponse sendResponse = myHighLoadWalletV3.deploy(config);
    Assertions.assertThat(sendResponse.getCode()).isZero();

    myHighLoadWalletV3.waitForDeployment(60);

    String singleRandomAddress = Utils.generateRandomAddress(0);

    JettonMinter jettonMinterWallet =
            JettonMinter.builder()
                    .tonCenterClient(tonCenter)
                    .customAddress(Address.of("kQAN6TAGauShFKDQvZCwNb_EeTUIjQDwRZ9t6GOn4FBzfg9Y"))
                    .build();

    JettonWallet myJettonHighLoadWallet =
            jettonMinterWallet.getJettonWallet(myHighLoadWalletV3.getAddress());

    config =
            HighloadV3Config.builder()
                    .walletId(42)
                    .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
                    .body(
                            myHighLoadWalletV3.createBulkTransfer(
                                    Collections.singletonList(
                                            Destination.builder()
                                                    .address(myJettonHighLoadWallet.getAddress().toBounceable())
                                                    .amount(Utils.toNano(0.07))
                                                    .body(
                                                            JettonWallet.createTransferBody(
                                                                    0,
                                                                    BigInteger.valueOf(100),
                                                                    Address.of(singleRandomAddress), // recipient
                                                                    myJettonHighLoadWallet.getAddress(), // response address
                                                                    null, // custom payload
                                                                    BigInteger.ONE, // forward amount
                                                                    MsgUtils.createTextMessageBody("test sdk") // forward payload
                                                            ))
                                                    .build()),
                                    BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
                    .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
                    .build();

    sendResponse = myHighLoadWalletV3.send(config);
    Assertions.assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(60, "sending jettons...");

    BigInteger balanceOfDestinationWallet =
            tonCenter.getBalance(Address.of(singleRandomAddress).toBounceable());
    log.info("balanceOfDestinationWallet in nanocoins: {}", balanceOfDestinationWallet);

    JettonWallet randomJettonWallet =
            jettonMinterWallet.getJettonWallet(Address.of(singleRandomAddress));
    log.info("balanceOfDestinationWallet in jettons: {}", randomJettonWallet.getBalance());
  }

  @Test
  public void testBulkTransferSimplified_TwoDestinationsTonCenter() throws Exception {
    TonCenter tonCenter =
            TonCenter.builder()
                    .apiKey(TESTNET_API_KEY)
                    .network(Network.TESTNET)
                    .build();
    Secp256k1KeyPair keyPair = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S contract =
            HighloadWalletV3S.builder().tonCenterClient(tonCenter).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getPrivateKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
            TestnetFaucet.topUpContract(tonCenter, Address.of(nonBounceableAddress), Utils.toNano(0.5));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    HighloadV3Config config =
            HighloadV3Config.builder()
                    .walletId(42)
                    .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
                    // .createdAt(createdAt) // default = now - 60 seconds
                    // .timeOut(60) //default timeout = 5 minutes
                    .build();

    SendResponse sendResponse = contract.deploy(config);
    log.info("extMessageInfo {}", sendResponse);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(45);
    log.info("deployed");

    int queryId = HighloadQueryId.fromSeqno(1).getQueryId();
    config =
            HighloadV3Config.builder()
                    .walletId(42)
                    .queryId(queryId)
                    .body(
                            contract.createBulkTransfer(
                                    Arrays.asList(
                                            Destination.builder()
                                                    .address("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO")
                                                    .amount(Utils.toNano(0.12))
                                                    .body(
                                                            CellBuilder.beginCell()
                                                                    .storeUint(0, 32)
                                                                    .storeString("test-comment-1")
                                                                    .endCell())
                                                    .build(),
                                            Destination.builder()
                                                    .address("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G")
                                                    .amount(Utils.toNano(0.11))
                                                    .comment("test-comment-2")
                                                    .build()),
                                    BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
                    .build();

    sendResponse = contract.send(config);
    log.info("extMessageInfo {}", sendResponse);
    assertThat(sendResponse.getCode()).isZero();
    log.info("sent 2 messages");
    Utils.sleep(2);
    String publicKey = contract.getPublicKey();
    log.info("public key {}", publicKey);

    Utils.sleep(2);
    long subWalletId = contract.getSubWalletId();
    log.info("subWalletId key {}", subWalletId);
    Utils.sleep(2);
    long lastCleanTime = contract.getLastCleanTime(); // todo
    log.info("lastCleanTime {}", lastCleanTime);
    Utils.sleep(2);
    long timeout = contract.getTimeout();
    log.info("timeout {}", timeout);
    Utils.sleep(2);
    boolean isProcessed = contract.isProcessed(queryId, true);
    log.info("isProcessed with clean {}", isProcessed);
    Utils.sleep(2);
    isProcessed = contract.isProcessed(queryId, false);
    log.info("isProcessed without clean {}", isProcessed);
  }
}
