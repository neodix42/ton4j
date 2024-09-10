package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.types.WalletV5Config;
import org.ton.java.smartcontract.types.WalletV5InnerRequest;
import org.ton.java.smartcontract.wallet.v3.WalletV3R1;
import org.ton.java.smartcontract.wallet.v5.WalletV5;
import org.ton.java.tlb.types.ActionList;
import org.ton.java.tlb.types.ExtendedAction;
import org.ton.java.tlb.types.ExtendedActionType;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class TestWalletV5 extends CommonTest {
    private static final Address addr1 = Address.of("EQA5f6BmWqizXLBPUKcPsKVmxpR17wdEfpRNzdxN9yw3zOru");
    private static final Address addr2 = Address.of("EQDf2t1GJQFqWDQYwoVHQMLLUQ7H8Sd1fP3ywN5lx_rlWXGB");
    private static final Address addr3 = Address.of("EQCnDZicIwOnbfHfuadO4P3Hl43MOs_1FpWqbqAz7mA-q3RO");

    /**
     * Wallet V5 deploy
     * Without Library and Without Extensions.
     */
    @Test
    public void testWalletV5Deployment() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 contract = WalletV5.builder()
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
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

    /**
     * Transfer to 1 recipient.
     * Without Library and Without Extensions and without OtherActions.
     */
    @Test
    public void testWalletV5SimpleTransfer1() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 contract = WalletV5.builder()
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // deploy wallet-v5
        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(60);

        long newSeq = contract.getSeqno();
        assertThat(newSeq).isEqualTo(1);

        WalletV5Config walletV5Config = WalletV5Config.builder()
                .seqno(newSeq)
                .walletId(42)
                .body(contract.createBulkTransfer(Collections.singletonList(Destination.builder()
                        .bounce(false)
                        .address(addr1.toBounceable())
                        .amount(Utils.toNano(0.0001))
                        .build())).toCell())
                .build();

        extMessageInfo = contract.send(walletV5Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    /**
     * Transfer to 255 recipients.
     * Without Library and Without Extensions and without OtherActions.
     */
    @Test
    public void testWalletV5SimpleTransfer255Recipients() throws InterruptedException, NoSuchAlgorithmException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 contract = WalletV5.builder()
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1.5));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // deploy wallet-v5
        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(60);

        long newSeq = contract.getSeqno();
        assertThat(newSeq).isEqualTo(1);

        Cell extMsg = contract.createBulkTransfer(createDummyDestinations(255)).toCell();

        WalletV5Config walletV5Config = WalletV5Config.builder()
                .seqno(newSeq)
                .walletId(42)
                .body(extMsg)
                .build();

        extMessageInfo = contract.send(walletV5Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    /**
     * Transfer to 0 recipient.
     * Without Library and Without Extensions and without OtherActions.
     */
    @Test
    public void testWalletV5SimpleTransfer0() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 contract = WalletV5.builder()
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // deploy wallet-v5
        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(60);

        long newSeq = contract.getSeqno();
        assertThat(newSeq).isEqualTo(1);

        WalletV5Config walletV5Config = WalletV5Config.builder()
                .seqno(newSeq)
                .walletId(42)
                .body(contract.createBulkTransfer(Collections.emptyList()).toCell())
                .build();

        extMessageInfo = contract.send(walletV5Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    /**
     * Deploy without extension and then add an extension.
     */
    @Test
    public void testWalletV5DeployOneExtension() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 contract = WalletV5.builder()
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // deploy wallet-v5
        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(60);

        long newSeq = contract.getSeqno();
        assertThat(newSeq).isEqualTo(1);

        WalletV5Config walletV5Config = WalletV5Config.builder()
                .seqno(newSeq)
                .walletId(42)
                .body(contract.manageExtensions(ActionList.builder()
                                .actions(Collections.singletonList(
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

    /**
     * Deploy without extension and then add two extensions.
     */
    @Test
    public void testWalletV5DeployTwoExtensions() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 contract = WalletV5.builder()
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // deploy wallet-v5
        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(60);

        long newSeq = contract.getSeqno();
        assertThat(newSeq).isEqualTo(1);

        Cell extensions = contract.manageExtensions(ActionList.builder()
                        .actions(Arrays.asList(
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
        WalletV5InnerRequest walletV5InnerRequest = WalletV5InnerRequest.deserialize(CellSlice.beginParse(extensions));
        log.info("walletV5InnerRequest (deserialized) {}", walletV5InnerRequest);
        log.info("walletV5InnerRequest (serialized)   {}", walletV5InnerRequest.toCell().toHex());

        WalletV5Config walletV5Config = WalletV5Config.builder()
                .seqno(newSeq)
                .walletId(42)
                .body(extensions)
                .build();

        extMessageInfo = contract.send(walletV5Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(15);
        log.info("extensions {}", contract.getRawExtensions());
        assertThat(contract.getRawExtensions().elements.size()).isEqualTo(2);
    }

    @Test
    public void testWalletV5GetExtensionsResult() {
        Cell cell = Cell.fromBoc("b5ee9c72010101010024000043a0137e94d888e4cc5032834f1860ba4118a993db9054f19ae1559c35cb29ee47ccf8");
        CellSlice cs = CellSlice.beginParse(cell);

        log.info("e {}", cs.loadDict(256,
                k -> k.readUint(256),
                v -> v));
    }

    @Test
    public void testWalletV5DeployWithOneExtension() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        // initial extensions
        TonHashMapE initExtensions = new TonHashMapE(256);
        initExtensions.elements.put(addr1.toBigInteger(), true);

        WalletV5 contract = WalletV5.builder()
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
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

        WalletV5 contract = WalletV5.builder()
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
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

        WalletV5 contract = WalletV5.builder()
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
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

        Cell extensionsToRemove = contract.manageExtensions(ActionList.builder()
                        .actions(Collections.singletonList(
                                ExtendedAction.builder()
                                        .actionType(ExtendedActionType.REMOVE_EXTENSION)
                                        .address(Address.of(addr2))
                                        .build()))
                        .build())
                .toCell();

        WalletV5Config walletV5Config = WalletV5Config.builder()
                .seqno(1)
                .walletId(42)
                .body(extensionsToRemove)
                .build();

        extMessageInfo = contract.send(walletV5Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(15);

        log.info("extensions {}", contract.getRawExtensions());
        assertThat(contract.getRawExtensions().elements.size()).isEqualTo(2);
    }

    /**
     * <pre>
     * On deployment installs two extensions, then sends a single request to:
     * - delete one extension,
     * - add one extension
     * - do a simple transfer.
     * </pre>
     */
    @Test
    public void testWalletV5DeployWithTwoExtensionsAndDeleteOneExtensionAndSendTransfer() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        // initial extensions
        TonHashMapE initExtensions = new TonHashMapE(256);
        initExtensions.elements.put(addr1.toBigInteger(), true);
        initExtensions.elements.put(addr2.toBigInteger(), false);

        WalletV5 contract = WalletV5.builder()
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
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

        Cell transferAndManageExtensions = contract.createBulkTransferAndManageExtensions(
                        Collections.singletonList(Destination.builder() // transfer
                                .bounce(false)
                                .address(addr1.toBounceable())
                                .amount(Utils.toNano(0.0001))
                                .build()),
                        ActionList.builder() // manage extensions
                                .actions(Arrays.asList(
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

        WalletV5Config walletV5Config = WalletV5Config.builder()
                .seqno(1)
                .walletId(42)
                .body(transferAndManageExtensions)
                .build();

        extMessageInfo = contract.send(walletV5Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(15);

        log.info("extensions {}", contract.getRawExtensions());
        assertThat(contract.getRawExtensions().elements.size()).isEqualTo(2);
    }

    /**
     * Deploy without extension and modify is signature auth allowed flag
     */
    @Test
    public void testWalletV5ModifySignatureAuthAllowed() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 contract = WalletV5.builder()
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
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

        WalletV5Config walletV5Config = WalletV5Config.builder()
                .seqno(newSeq)
                .walletId(42)
                .body(contract.manageExtensions(ActionList.builder()
                                .actions(Collections.singletonList(
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

        // Signature Auth flag is not changed, since - only_extension_can_change_signature_mode, error 146
        assertThat(contract.getIsSignatureAuthAllowed()).isTrue();
    }


    /**
     * Test internal transfer to 2 recipient.
     * Sending request from v3r1 to v5r1 to perform from v5r1 two txs - to addr1 and addr2.
     * internal_signed#73696e74 signed:SignedRequest = InternalMsgBody;
     * Without Library and Without Extensions and without OtherActions.
     */
    @Test
    public void testWalletV5InternalTransfer1() throws InterruptedException {

        // create user wallet that sends an internal message to wallet v5

        WalletV3R1 contractV3 = WalletV3R1.builder()
                .tonlib(tonlib)
                .walletId(43)
                .build();

        Address walletAddressV3 = contractV3.getAddress();

        String nonBounceableAddress = walletAddressV3.toNonBounceable();
        String bounceableAddress = walletAddressV3.toBounceable();
        log.info("bounceableAddress v3: {}", bounceableAddress);
        log.info("pub key: {}", Utils.bytesToHex(contractV3.getKeyPair().getPublicKey()));
        log.info("prv key: {}", Utils.bytesToHex(contractV3.getKeyPair().getSecretKey()));

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.2));
        log.info("walletId {} new wallet v3 {} balance: {}", contractV3.getWalletId(), contractV3.getName(), Utils.formatNanoValue(balance));

        // deploy wallet v3
        ExtMessageInfo extMessageInfo = contractV3.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();
        contractV3.waitForDeployment(60);


        // create wallet v5
        TweetNaclFast.Signature.KeyPair keyPairV5 = Utils.generateSignatureKeyPair();
        WalletV5 contractV5 = WalletV5.builder()
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

        balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.2));
        log.info("new wallet v5 {} balance: {}", contractV5.getName(), Utils.formatNanoValue(balance));

        // deploy wallet v5
        extMessageInfo = contractV5.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contractV5.waitForDeployment(60);

        // internal payload for wallet v5
        WalletV5Config walletV5Config = WalletV5Config.builder()
                .seqno(1)
                .walletId(42)
                .body(contractV5.createBulkTransfer(Arrays.asList(Destination.builder()
                                .bounce(false)
                                .address(addr1.toNonBounceable())
                                .amount(Utils.toNano(0.013))
                                .build(),
                        Destination.builder()
                                .bounce(false)
                                .address(addr2.toNonBounceable())
                                .amount(Utils.toNano(0.015))
                                .build())).toCell())
                .build();

        WalletV3Config walletV3Config = WalletV3Config.builder()
                .seqno(1)
                .walletId(43)
                .destination(contractV5.getAddress())
                .amount(Utils.toNano(0.017))
                .body(contractV5.createInternalSignedlBody(walletV5Config))
                .build();

        extMessageInfo = contractV3.send(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    List<Destination> createDummyDestinations(int count) throws NoSuchAlgorithmException {
        List<Destination> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String dstDummyAddress = "0:" + Utils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));

            result.add(Destination.builder()
                    .bounce(false)
                    .address(dstDummyAddress)
                    .amount(Utils.toNano(0.0001))
                    .comment("memo " + (i + 1))
                    .build());
        }
        return result;
    }
}
