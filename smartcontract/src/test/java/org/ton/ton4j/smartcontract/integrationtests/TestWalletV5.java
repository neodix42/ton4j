package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.faucet.TestnetJettonFaucet;
import org.ton.ton4j.smartcontract.token.ft.JettonMinter;
import org.ton.ton4j.smartcontract.token.ft.JettonWallet;
import org.ton.ton4j.smartcontract.types.Destination;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.types.WalletV5Config;
import org.ton.ton4j.smartcontract.types.WalletV5InnerRequest;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.ContractUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R1;
import org.ton.ton4j.smartcontract.wallet.v5.WalletV5;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class TestWalletV5 extends CommonTest {
  private static final Address addr1 =
      Address.of("EQA5f6BmWqizXLBPUKcPsKVmxpR17wdEfpRNzdxN9yw3zOru");
  private static final Address addr2 =
      Address.of("EQDf2t1GJQFqWDQYwoVHQMLLUQ7H8Sd1fP3ywN5lx_rlWXGB");
  private static final Address addr3 =
      Address.of("EQCnDZicIwOnbfHfuadO4P3Hl43MOs_1FpWqbqAz7mA-q3RO");

  /** Wallet V5 deploy Without Library and Without Extensions. */
  @Test
  public void testWalletV5Deployment() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    //        byte[] secretKey =
    // Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    //        TweetNaclFast.Signature.KeyPair keyPair =
    // TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    assertThat(contract.getSeqno()).isEqualTo(1);

    log.info("walletId {}", contract.getWalletId());
    log.info("publicKey {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("isSignatureAuthAllowed {}", contract.getIsSignatureAuthAllowed());
    log.info("extensions {}", contract.getRawExtensions());
  }

  /** Transfer to 1 recipient. Without Library and Without Extensions and without OtherActions. */
  @Test
  public void testWalletV5SimpleTransfer1() throws InterruptedException {
    //        byte[] secretKey =
    // Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    //        TweetNaclFast.Signature.KeyPair keyPair =
    // TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("rawAddress: {}", walletAddress.toRaw());
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    long newSeq = contract.getSeqno();
    assertThat(newSeq).isEqualTo(1);

    WalletV5Config walletV5Config =
        WalletV5Config.builder()
            .seqno(newSeq)
            .walletId(42)
            .body(
                contract
                    .createBulkTransfer(
                        Collections.singletonList(
                            Destination.builder()
                                .bounce(true)
                                .address(
                                    "0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d")
                                .mode(3)
                                .amount(Utils.toNano(0.05))
                                .comment("gift")
                                .build()))
                    .toCell())
            //                .validUntil(1753376922)
            .build();

    Message msg = contract.prepareExternalMsg(walletV5Config);
    log.info("msg {}", msg.toCell().toHex());

    extMessageInfo = contract.send(walletV5Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
  }

  /**
   * Transfer to 255 recipients. Without Library and Without Extensions and without OtherActions.
   */
  @Test
  public void testWalletV5SimpleTransfer255Recipients()
      throws InterruptedException, NoSuchAlgorithmException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1.5));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    long newSeq = contract.getSeqno();
    assertThat(newSeq).isEqualTo(1);

    Cell extMsg = contract.createBulkTransfer(createDummyDestinations(255)).toCell();

    WalletV5Config walletV5Config =
        WalletV5Config.builder().seqno(newSeq).walletId(42).body(extMsg).build();

    extMessageInfo = contract.send(walletV5Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
  }

  /** Transfer to 0 recipient. Without Library and Without Extensions and without OtherActions. */
  @Test
  public void testWalletV5SimpleTransfer0() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    long newSeq = contract.getSeqno();
    assertThat(newSeq).isEqualTo(1);

    WalletV5Config walletV5Config =
        WalletV5Config.builder()
            .seqno(newSeq)
            .walletId(42)
            .body(contract.createBulkTransfer(Collections.emptyList()).toCell())
            .build();

    extMessageInfo = contract.send(walletV5Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
  }

  /** Deploy without extension and then add an extension. */
  @Test
  public void testWalletV5DeployOneExtension() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .isSigAuthAllowed(true)
            .walletId(42)
            .keyPair(keyPair)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    long newSeq = contract.getSeqno();
    assertThat(newSeq).isEqualTo(1);

    WalletV5Config walletV5Config =
        WalletV5Config.builder()
            .seqno(newSeq)
            .walletId(42)
            .body(
                contract
                    .manageExtensions(
                        ActionList.builder()
                            .actions(
                                Collections.singletonList(
                                    ExtendedAction.builder()
                                        .actionType(ExtendedActionType.ADD_EXTENSION)
                                        .address(Address.of(addr2))
                                        .build()))
                            .build())
                    .toCell())
            .build();

    extMessageInfo = contract.send(walletV5Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    Utils.sleep(15);
    log.info("extensions {}", contract.getRawExtensions());
    assertThat(contract.getRawExtensions().elements.size()).isEqualTo(1);
  }

  /** Deploy without extension and then add two extensions. */
  @Test
  public void testWalletV5DeployTwoExtensions() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    long newSeq = contract.getSeqno();
    assertThat(newSeq).isEqualTo(1);

    Cell extensions =
        contract
            .manageExtensions(
                ActionList.builder()
                    .actions(
                        Arrays.asList(
                            ExtendedAction.builder()
                                .actionType(ExtendedActionType.ADD_EXTENSION)
                                .address(Address.of(addr2))
                                .build(),
                            ExtendedAction.builder()
                                .actionType(ExtendedActionType.ADD_EXTENSION)
                                .address(Address.of(addr3))
                                .build()))
                    .build())
            .toCell();

    // test WalletV5InnerRequest de/serialization
    WalletV5InnerRequest walletV5InnerRequest =
        WalletV5InnerRequest.deserialize(CellSlice.beginParse(extensions));
    log.info("walletV5InnerRequest (deserialized) {}", walletV5InnerRequest);
    log.info("walletV5InnerRequest (serialized)   {}", walletV5InnerRequest.toCell().toHex());

    WalletV5Config walletV5Config =
        WalletV5Config.builder().seqno(newSeq).walletId(42).body(extensions).build();

    extMessageInfo = contract.send(walletV5Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    Utils.sleep(15);
    log.info("extensions {}", contract.getRawExtensions());
    assertThat(contract.getRawExtensions().elements.size()).isEqualTo(2);
  }

  @Test
  public void testWalletV5GetExtensionsResult() {
    Cell cell =
        Cell.fromBoc(
            "b5ee9c72010101010024000043a0137e94d888e4cc5032834f1860ba4118a993db9054f19ae1559c35cb29ee47ccf8");
    CellSlice cs = CellSlice.beginParse(cell);

    log.info("e {}", cs.loadDict(256, k -> k.readUint(256), v -> v));
  }

  @Test
  public void testWalletV5DeployWithOneExtension() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    // initial extensions
    TonHashMapE initExtensions = new TonHashMapE(256);
    initExtensions.elements.put(addr1.toBigInteger(), true);

    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .isSigAuthAllowed(true)
            .keyPair(keyPair)
            .extensions(initExtensions)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    assertThat(contract.getSeqno()).isEqualTo(1);

    log.info("walletId {}", contract.getWalletId());
    log.info("publicKey {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("isSignatureAuthAllowed {}", contract.getIsSignatureAuthAllowed());
    log.info("extensions {}", contract.getRawExtensions());
    assertThat(contract.getRawExtensions().elements.size()).isEqualTo(1);
  }

  @Test
  public void testWalletV5DeployWithThreeExtensions() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    // initial extensions
    TonHashMapE initExtensions = new TonHashMapE(256);
    initExtensions.elements.put(addr1.toBigInteger(), true);
    initExtensions.elements.put(addr2.toBigInteger(), true);
    initExtensions.elements.put(addr3.toBigInteger(), false);

    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .isSigAuthAllowed(true)
            .keyPair(keyPair)
            .extensions(initExtensions)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    assertThat(contract.getSeqno()).isEqualTo(1);

    log.info("walletId {}", contract.getWalletId());
    log.info("publicKey {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("isSignatureAuthAllowed {}", contract.getIsSignatureAuthAllowed());
    log.info("extensions {}", contract.getRawExtensions());
    assertThat(contract.getRawExtensions().elements.size()).isEqualTo(3);
  }

  @Test
  public void testWalletV5DeployWithThreeExtensionsAndDeleteOne() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    // initial extensions
    TonHashMapE initExtensions = new TonHashMapE(256);
    initExtensions.elements.put(addr1.toBigInteger(), true);
    initExtensions.elements.put(addr2.toBigInteger(), true);
    initExtensions.elements.put(addr3.toBigInteger(), false);

    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .isSigAuthAllowed(true)
            .keyPair(keyPair)
            .extensions(initExtensions)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    assertThat(contract.getSeqno()).isEqualTo(1);

    log.info("walletId {}", contract.getWalletId());
    log.info("publicKey {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("isSignatureAuthAllowed {}", contract.getIsSignatureAuthAllowed());
    log.info("extensions {}", contract.getRawExtensions());

    assertThat(contract.getRawExtensions().elements.size()).isEqualTo(3);

    Cell extensionsToRemove =
        contract
            .manageExtensions(
                ActionList.builder()
                    .actions(
                        Collections.singletonList(
                            ExtendedAction.builder()
                                .actionType(ExtendedActionType.REMOVE_EXTENSION)
                                .address(Address.of(addr2))
                                .build()))
                    .build())
            .toCell();

    WalletV5Config walletV5Config =
        WalletV5Config.builder().seqno(1).walletId(42).body(extensionsToRemove).build();

    extMessageInfo = contract.send(walletV5Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    Utils.sleep(15);

    log.info("extensions {}", contract.getRawExtensions());
    assertThat(contract.getRawExtensions().elements.size()).isEqualTo(2);
  }

  /**
   *
   *
   * <pre>
   * On deployment installs two extensions, then sends a single request to:
   * - delete one extension,
   * - add one extension
   * - do a simple transfer.
   * </pre>
   */
  @Test
  public void testWalletV5DeployWithTwoExtensionsAndDeleteOneExtensionAndSendTransfer()
      throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    // initial extensions
    TonHashMapE initExtensions = new TonHashMapE(256);
    initExtensions.elements.put(addr1.toBigInteger(), true);
    initExtensions.elements.put(addr2.toBigInteger(), false);

    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .isSigAuthAllowed(true)
            .keyPair(keyPair)
            .extensions(initExtensions)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    assertThat(contract.getSeqno()).isEqualTo(1);

    log.info("walletId {}", contract.getWalletId());
    log.info("publicKey {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("isSignatureAuthAllowed {}", contract.getIsSignatureAuthAllowed());
    log.info("extensions {}", contract.getRawExtensions());

    assertThat(contract.getRawExtensions().elements.size()).isEqualTo(2);

    Cell transferAndManageExtensions =
        contract
            .createBulkTransferAndManageExtensions(
                Collections.singletonList(
                    Destination.builder() // transfer
                        .bounce(false)
                        .address(addr1.toBounceable())
                        .amount(Utils.toNano(0.0001))
                        .build()),
                ActionList.builder() // manage extensions
                    .actions(
                        Arrays.asList(
                            ExtendedAction.builder()
                                .actionType(ExtendedActionType.REMOVE_EXTENSION)
                                .address(Address.of(addr2))
                                .build(),
                            ExtendedAction.builder()
                                .actionType(ExtendedActionType.ADD_EXTENSION)
                                .address(Address.of(addr3))
                                .build()))
                    .build())
            .toCell();

    WalletV5Config walletV5Config =
        WalletV5Config.builder().seqno(1).walletId(42).body(transferAndManageExtensions).build();

    extMessageInfo = contract.send(walletV5Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    Utils.sleep(15);

    log.info("extensions {}", contract.getRawExtensions());
    assertThat(contract.getRawExtensions().elements.size()).isEqualTo(2);
  }

  /**
   * Disallow signature authentication. Deploy without extension and modify is signature auth
   * allowed flag.
   */
  @Test
  public void testWalletV5ModifySignatureAuthAllowed() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .isSigAuthAllowed(true)
            .walletId(42)
            .keyPair(keyPair)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    long newSeq = contract.getSeqno();
    assertThat(newSeq).isEqualTo(1);

    log.info("walletId {}", contract.getWalletId());
    log.info("publicKey {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("isSignatureAuthAllowed {}", contract.getIsSignatureAuthAllowed());
    log.info("extensions {}", contract.getRawExtensions());

    WalletV5Config walletV5Config =
        WalletV5Config.builder()
            .seqno(newSeq)
            .walletId(42)
            .body(
                contract
                    .manageExtensions(
                        ActionList.builder()
                            .actions(
                                Collections.singletonList(
                                    ExtendedAction.builder()
                                        .actionType(ExtendedActionType.SET_SIGNATURE_AUTH_FLAG)
                                        .isSignatureAllowed(false)
                                        .build()))
                            .build())
                    .toCell())
            .build();

    extMessageInfo = contract.send(walletV5Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    Utils.sleep(15);

    log.info("walletId {}", contract.getWalletId());
    log.info("publicKey {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("isSignatureAuthAllowed {}", contract.getIsSignatureAuthAllowed());
    log.info("extensions {}", contract.getRawExtensions());

    // Signature Auth flag is not changed, since - only_extension_can_change_signature_mode, error
    // 146
    assertThat(contract.getIsSignatureAuthAllowed()).isTrue();
  }

  /**
   * Test internal transfer to 2 recipient. Sending request from v3r1 to v5r1 to perform from v5r1
   * two txs - to addr1 and addr2. internal_signed#73696e74 signed:SignedRequest = InternalMsgBody;
   * As you may notice v3r1 can use v5r1's funds, but needs a signature from v5r1.
   */
  @Test
  public void testWalletV5InternalTransfer1() throws InterruptedException {

    // create user wallet that sends an internal message to wallet v5

    WalletV3R1 contractV3 = WalletV3R1.builder().tonlib(tonlib).walletId(43).build();

    Address walletAddressV3 = contractV3.getAddress();

    String nonBounceableAddress = walletAddressV3.toNonBounceable();
    String bounceableAddress = walletAddressV3.toBounceable();
    log.info("bounceableAddress v3: {}", bounceableAddress);
    log.info("pub key: {}", Utils.bytesToHex(contractV3.getKeyPair().getPublicKey()));
    log.info("prv key: {}", Utils.bytesToHex(contractV3.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.2));
    log.info(
        "walletId {} new wallet v3 {} balance: {}",
        contractV3.getWalletId(),
        contractV3.getName(),
        Utils.formatNanoValue(balance));

    // deploy wallet v3
    ExtMessageInfo extMessageInfo = contractV3.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contractV3.waitForDeployment(60);

    // create wallet v5
    TweetNaclFast.Signature.KeyPair keyPairV5 = Utils.generateSignatureKeyPair();
    WalletV5 contractV5 =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPairV5)
            .isSigAuthAllowed(true)
            .build();

    Address walletAddressV5 = contractV5.getAddress();

    nonBounceableAddress = walletAddressV5.toNonBounceable();
    bounceableAddress = walletAddressV5.toBounceable();
    log.info("bounceableAddress v5: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contractV5.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contractV5.getKeyPair().getSecretKey()));

    balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.2));
    log.info("new wallet v5 {} balance: {}", contractV5.getName(), Utils.formatNanoValue(balance));

    // deploy wallet v5
    extMessageInfo = contractV5.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contractV5.waitForDeployment(60);

    // internal payload for wallet v5
    WalletV5Config walletV5Config =
        WalletV5Config.builder()
            .seqno(1)
            .walletId(42)
            .body(
                contractV5
                    .createBulkTransfer(
                        Arrays.asList(
                            Destination.builder()
                                .bounce(false)
                                .address(addr1.toNonBounceable())
                                .amount(Utils.toNano(0.013))
                                .build(),
                            Destination.builder()
                                .bounce(false)
                                .address(addr2.toNonBounceable())
                                .amount(Utils.toNano(0.015))
                                .build()))
                    .toCell())
            .build();

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .seqno(1)
            .walletId(43)
            .destination(contractV5.getAddress())
            .amount(Utils.toNano(0.017))
            .body(contractV5.createInternalSignedBody(walletV5Config))
            .build();

    extMessageInfo = contractV3.send(walletV3Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
  }

  /**
   * Test internal extension transfer to 2 recipient. Sending request from v3r1 to v5r1 to perform
   * from v5r1 two txs - to addr1 and addr2. internal_extension#6578746e query_id:(## 64)
   * inner:InnerRequest = InternalMsgBody; As you may notice v3r1 can use v5r1's funds, WITHOUT a
   * signature from v5r1, since v3r1 is an extension of v5r1.
   */
  @Test
  public void testWalletV5TransferFromExtension() throws InterruptedException {

    // create a wallet v3r1 that will be an extension

    WalletV3R1 contractV3 = WalletV3R1.builder().tonlib(tonlib).walletId(43).build();

    Address walletAddressV3 = contractV3.getAddress();

    String nonBounceableAddress = walletAddressV3.toNonBounceable();
    String bounceableAddress = walletAddressV3.toBounceable();
    log.info("bounceableAddress v3: {}", bounceableAddress);
    log.info("pub key: {}", Utils.bytesToHex(contractV3.getKeyPair().getPublicKey()));
    log.info("prv key: {}", Utils.bytesToHex(contractV3.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.2));
    log.info(
        "walletId {} new wallet v3 {} balance: {}",
        contractV3.getWalletId(),
        contractV3.getName(),
        Utils.formatNanoValue(balance));

    // deploy wallet v3
    ExtMessageInfo extMessageInfo = contractV3.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contractV3.waitForDeployment(60);

    // create wallet v5 with initial extension (which is v3r1 wallet)
    TonHashMapE initExtensions = new TonHashMapE(256);
    initExtensions.elements.put(contractV3.getAddress().toBigInteger(), true);

    TweetNaclFast.Signature.KeyPair keyPairV5 = Utils.generateSignatureKeyPair();
    WalletV5 contractV5 =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPairV5)
            .isSigAuthAllowed(true)
            .extensions(initExtensions) // assign wallet v3r1 as extension of wallet v5r1
            .build();

    Address walletAddressV5 = contractV5.getAddress();

    nonBounceableAddress = walletAddressV5.toNonBounceable();
    bounceableAddress = walletAddressV5.toBounceable();
    log.info("bounceableAddress v5: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contractV5.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contractV5.getKeyPair().getSecretKey()));

    balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.2));
    log.info("new wallet v5 {} balance: {}", contractV5.getName(), Utils.formatNanoValue(balance));

    // deploy wallet v5
    extMessageInfo = contractV5.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contractV5.waitForDeployment(60);
    assertThat(contractV5.getRawExtensions().elements.size()).isEqualTo(1);

    BigInteger queryId = BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60L << 32);

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .seqno(1)
            .walletId(43)
            .destination(contractV5.getAddress())
            .amount(Utils.toNano(0.017))
            .body(
                contractV5.createInternalExtensionTransferBody(
                    queryId,
                    contractV5
                        .createBulkTransfer(
                            Arrays.asList(
                                Destination.builder()
                                    .bounce(false)
                                    .address(addr1.toNonBounceable())
                                    .amount(Utils.toNano(0.013))
                                    .build(),
                                Destination.builder()
                                    .bounce(false)
                                    .address(addr2.toNonBounceable())
                                    .amount(Utils.toNano(0.015))
                                    .build()))
                        .toCell()))
            .build();

    extMessageInfo = contractV3.send(walletV3Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
  }

  /** Test removal of extensions from other extension. */
  @Test
  public void testWalletV5ManageExtensionsFromExtension() throws InterruptedException {

    // create a wallet v3r1 that will be an extension

    WalletV3R1 contractV3 = WalletV3R1.builder().tonlib(tonlib).walletId(43).build();

    Address walletAddressV3 = contractV3.getAddress();

    String nonBounceableAddress = walletAddressV3.toNonBounceable();
    String bounceableAddress = walletAddressV3.toBounceable();
    log.info("bounceableAddress v3: {}", bounceableAddress);
    log.info("pub key: {}", Utils.bytesToHex(contractV3.getKeyPair().getPublicKey()));
    log.info("prv key: {}", Utils.bytesToHex(contractV3.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.2));
    log.info(
        "walletId {} new wallet v3 {} balance: {}",
        contractV3.getWalletId(),
        contractV3.getName(),
        Utils.formatNanoValue(balance));

    // deploy wallet v3
    ExtMessageInfo extMessageInfo = contractV3.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contractV3.waitForDeployment(60);

    // create wallet v5 with initial extension (which is v3r1 wallet)
    TonHashMapE initExtensions = new TonHashMapE(256);
    initExtensions.elements.put(contractV3.getAddress().toBigInteger(), true);
    initExtensions.elements.put(addr1.toBigInteger(), true);
    initExtensions.elements.put(addr2.toBigInteger(), false);

    TweetNaclFast.Signature.KeyPair keyPairV5 = Utils.generateSignatureKeyPair();
    WalletV5 contractV5 =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPairV5)
            .isSigAuthAllowed(true)
            .extensions(initExtensions) // assign wallet v3r1 as extension of wallet v5r1
            .build();

    Address walletAddressV5 = contractV5.getAddress();

    nonBounceableAddress = walletAddressV5.toNonBounceable();
    bounceableAddress = walletAddressV5.toBounceable();
    log.info("bounceableAddress v5: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contractV5.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contractV5.getKeyPair().getSecretKey()));

    balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.2));
    log.info("new wallet v5 {} balance: {}", contractV5.getName(), Utils.formatNanoValue(balance));

    // deploy wallet v5
    extMessageInfo = contractV5.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contractV5.waitForDeployment(60);
    log.info("extensions {}", contractV5.getRawExtensions());
    assertThat(contractV5.getRawExtensions().elements.size()).isEqualTo(3);

    BigInteger queryId = BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60L << 32);

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .seqno(1)
            .walletId(43)
            .destination(contractV5.getAddress())
            .amount(Utils.toNano(0.017))
            .body(
                contractV5.createInternalExtensionTransferBody(
                    queryId,
                    contractV5
                        .manageExtensions(
                            ActionList.builder()
                                .actions(
                                    Collections.singletonList(
                                        ExtendedAction.builder()
                                            .actionType(ExtendedActionType.REMOVE_EXTENSION)
                                            .address(Address.of(addr2))
                                            .build()))
                                .build())
                        .toCell()))
            .build();

    extMessageInfo = contractV3.send(walletV3Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    Utils.sleep(20);
    log.info("extensions {}", contractV5.getRawExtensions());
    assertThat(contractV5.getRawExtensions().elements.size()).isEqualTo(2);
  }

  /** Wallet V5 deploy Without Library and Without Extensions. */
  @Test
  public void testWalletV5DeploymentAsLibrary() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .deployAsLibrary(true)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);
  }

  /** Use wallet v5 as library. See TestLibraryDeployer.java for library deployment. */
  @Test
  public void testWalletV5DeployAsLibrary() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();
    Utils.sleep(15, "takes longer...");
    assertThat(contract.getSeqno()).isEqualTo(1);

    log.info("walletId {}", contract.getWalletId());
    log.info("publicKey {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("isSignatureAuthAllowed {}", contract.getIsSignatureAuthAllowed());
    log.info("extensions {}", contract.getRawExtensions());
  }

  /** Transfer to 1 recipient. With Library and Without Extensions and without OtherActions. */
  @Test
  public void testWalletV5SimpleTransfer1WhenDeployedAsLibrary() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .deployAsLibrary(true)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    long newSeq = contract.getSeqno();
    assertThat(newSeq).isEqualTo(1);

    WalletV5Config walletV5Config =
        WalletV5Config.builder()
            .seqno(newSeq)
            .walletId(42)
            .body(
                contract
                    .createBulkTransfer(
                        Collections.singletonList(
                            Destination.builder()
                                .bounce(false)
                                .address(addr1.toBounceable())
                                .amount(Utils.toNano(0.0001))
                                .build()))
                    .toCell())
            .build();

    extMessageInfo = contract.send(walletV5Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
  }

  @Test
  public void testWalletV5SimpleJettonTransfer() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 walletV5 =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .build();

    Address walletAddress = walletV5.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("rawAddress: {}", walletAddress.toRaw());
    log.info("pub-key {}", Utils.bytesToHex(walletV5.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(walletV5.getKeyPair().getSecretKey()));

    // top up wallet-v5 with some toncoins before deployment
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", walletV5.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    ExtMessageInfo extMessageInfo = walletV5.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    walletV5.waitForDeployment(60);

    long newSeq = walletV5.getSeqno();
    assertThat(newSeq).isEqualTo(1);

    // top up new wallet-v5 with 100 NEOJ jettons using test-jetton-faucet-wallet
    balance =
        TestnetJettonFaucet.topUpContractWithNeoj(
            tonlib, Address.of(nonBounceableAddress), BigInteger.valueOf(100));
    log.info(
        "new wallet {} jetton balance: {}",
        walletV5.getName(),
        Utils.formatJettonValue(balance, 2, 2));

    String neojMasterJettonContractAddress = "kQAN6TAGauShFKDQvZCwNb_EeTUIjQDwRZ9t6GOn4FBzfg9Y";
    JettonMinter jettonMinterWallet =
        JettonMinter.builder()
            .tonlib(tonlib)
            .customAddress(Address.of(neojMasterJettonContractAddress))
            .build();

    // two ways to show balance in jettons

    // way first
    JettonWallet myJettonWallet = jettonMinterWallet.getJettonWallet(walletV5.getAddress());
    log.info(
        "walletV5 ({}) balance in jettons {}",
        walletV5.getAddress().toRaw(),
        myJettonWallet.getBalance());

    // way second
    BigInteger balance1 =
        ContractUtils.getJettonBalance(
            tonlib, Address.of(neojMasterJettonContractAddress), walletV5.getAddress());
    log.info("walletV5 ({}) balance in jettons {}", walletV5.getAddress().toRaw(), balance1);
    // send jettons from wallet-v5

    WalletV5Config walletV5Config =
        WalletV5Config.builder()
            .seqno(newSeq)
            .walletId(42)
            .body(
                walletV5
                    .createBulkTransfer(
                        Collections.singletonList(
                            Destination.builder()
                                .bounce(true)
                                .address(myJettonWallet.getAddress().toBounceable())
                                .mode(3)
                                .amount(Utils.toNano(0.05))
                                .body(
                                    JettonWallet.createTransferBody(
                                        0,
                                        BigInteger.valueOf(50), // amount of jettons to send
                                        Address.of(
                                            "0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"), // recipient
                                        myJettonWallet.getAddress(), // response address
                                        null, // custom payload
                                        BigInteger.ONE, // forward amount
                                        MsgUtils.createTextMessageBody(
                                            "v5-jetton-demo") // forward payload
                                        ))
                                .build()))
                    .toCell())
            .build();

    extMessageInfo = walletV5.send(walletV5Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    Utils.sleep(20);
    log.info(
        "walletV5 ({}) balance left in jettons {}",
        walletV5.getAddress().toRaw(),
        myJettonWallet.getBalance());
  }

  @Test
  public void testWalletV5SimpleTransfer_ExternallySigned() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] pubKey = keyPair.getPublicKey();
    WalletV5 contract =
        WalletV5.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("rawAddress: {}", walletAddress.toRaw());
    log.info("pub-key {}", Utils.bytesToHex(pubKey));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v5
    Cell deployBody = contract.createDeployMsg();
    byte[] signedDeployBodyHash = Utils.signData(pubKey, keyPair.getSecretKey(), deployBody.hash());

    ExtMessageInfo extMessageInfo = contract.deploy(signedDeployBodyHash);
    log.info("extMessageInfo: {}", extMessageInfo);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    long newSeq = contract.getSeqno();
    assertThat(newSeq).isEqualTo(1);

    WalletV5Config walletV5Config =
        WalletV5Config.builder()
            .seqno(newSeq)
            .walletId(42)
            .body(
                contract
                    .createBulkTransfer(
                        Collections.singletonList(
                            Destination.builder()
                                .bounce(true)
                                .address(
                                    "0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d")
                                .mode(3)
                                .amount(Utils.toNano(0.05))
                                .comment("ton4j-v5-externally-signed")
                                .build()))
                    .toCell())
            .build();

    Cell transferBody = contract.createExternalTransferBody(walletV5Config);
    byte[] signedTransferBodyHash =
        Utils.signData(pubKey, keyPair.getSecretKey(), transferBody.hash());
    extMessageInfo = contract.send(walletV5Config, signedTransferBodyHash);
    assertThat(extMessageInfo.getError().getCode()).isZero();
  }

  List<Destination> createDummyDestinations(int count) throws NoSuchAlgorithmException {
    List<Destination> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String dstDummyAddress = Utils.generateRandomAddress(0);

      result.add(
          Destination.builder()
              .bounce(false)
              .address(dstDummyAddress)
              .amount(Utils.toNano(0.0001))
              .comment("memo " + (i + 1))
              .build());
    }
    return result;
  }
}
