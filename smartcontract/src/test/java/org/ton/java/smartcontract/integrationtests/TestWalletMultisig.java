package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.multisig.MultisigWallet;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletMultisig extends CommonTest {

    TweetNaclFast.Signature.KeyPair ownerKeyPair = Utils.generateSignatureKeyPair();
    TweetNaclFast.Signature.KeyPair keyPair2 = Utils.generateSignatureKeyPair();
    TweetNaclFast.Signature.KeyPair keyPair3 = Utils.generateSignatureKeyPair();
    TweetNaclFast.Signature.KeyPair keyPair4 = Utils.generateSignatureKeyPair();
    TweetNaclFast.Signature.KeyPair keyPair5 = Utils.generateSignatureKeyPair();

    /**
     * Any user deploys a multisig wallet.
     * Any user from the list creates an order and gathers all the required signatures,
     * then sends the order to the wallet.
     */
    @Test
    public void testWalletMultisigOffline() throws InterruptedException {

        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
        log.info("pubKey2 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
        log.info("pubKey3 {}", Utils.bytesToHex(keyPair3.getPublicKey()));
        log.info("pubKey4 {}", Utils.bytesToHex(keyPair4.getPublicKey()));
        log.info("pubKey5 {}", Utils.bytesToHex(keyPair5.getPublicKey()));

        BigInteger queryId = BigInteger.valueOf(Instant.now().getEpochSecond() + 2 * 60 * 60L << 32);

        Long walletId = new Random().nextLong() & 0xffffffffL;
        log.info("queryId {}, walletId {}", queryId, walletId);

        int rootIndex = 0;
        int pubkey2Index = 1;
        int pubkey3Index = 2;
        int pubkey4Index = 3;
        int pubkey5Index = 4;
        int k = 3;
        int n = 5;

        Options options = Options.builder()
                .publicKey(ownerKeyPair.getPublicKey())
                .secretKey(ownerKeyPair.getSecretKey())
                .walletId(walletId)
                .multisigConfig(MultisigConfig.builder()
                        .queryId(queryId)
                        .k(k)
                        .n(n)
                        .rootI(rootIndex)
                        .owners(
                                List.of(
                                        OwnerInfo.builder()
                                                .publicKey(ownerKeyPair.getPublicKey())
                                                .flood(1)
                                                .build(),
                                        OwnerInfo.builder()
                                                .publicKey(keyPair2.getPublicKey())
                                                .flood(2)
                                                .build(),
                                        OwnerInfo.builder()
                                                .publicKey(keyPair3.getPublicKey())
                                                .flood(3)
                                                .build(),
                                        OwnerInfo.builder()
                                                .publicKey(keyPair4.getPublicKey())
                                                .flood(4)
                                                .build(),
                                        OwnerInfo.builder()
                                                .publicKey(keyPair5.getPublicKey())
                                                .flood(5)
                                                .build()
                                )
                        ).build())
                .build();

        MultisigWallet contract = new Wallet(WalletVersion.multisig, options).create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        Utils.sleep(30, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        MultisigWalletConfig config = MultisigWalletConfig.builder().build();

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, null);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying"); // with empty ext-msg

        log.info("owners publicKeys {}", contract.getPublicKeys(tonlib));
        log.info("owners publicKeysHex {}", contract.getPublicKeysHex(tonlib));

        Pair<Long, Long> n_k = contract.getNandK(tonlib);
        log.info("n {}, k {}", n_k.getLeft(), n_k.getRight());

        // You can include up to 3 destinations
        Cell msg1 = MultisigWallet.createOneInternalMsg(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);
        Cell msg2 = MultisigWallet.createOneInternalMsg(Address.of("EQDUna0j-TKlMU9pOBBHNoLzpwlewHl7S1qXtnaYdTTs_Ict"), Utils.toNano(0.6), 3);
        Cell msg3 = MultisigWallet.createOneInternalMsg(Address.of("EQCAy2ue54I-uDvEgD3qXdqjtrJI4F4OeFn3V10Kgt0jXpQn"), Utils.toNano(0.7), 3);

        // Having message(s) to send you can group it to a new order
        Cell order = MultisigWallet.createOrder(walletId, queryId, msg1, msg2, msg3);
        order.toFile("order.boc");

        byte[] orderSignatureUser1 = MultisigWallet.signOrder(ownerKeyPair, order);
        byte[] orderSignatureUser2 = MultisigWallet.signOrder(keyPair2, order);
        byte[] orderSignatureUser3 = MultisigWallet.signOrder(keyPair3, order);
        byte[] orderSignatureUser4 = MultisigWallet.signOrder(keyPair4, order);
        byte[] orderSignatureUser5 = MultisigWallet.signOrder(keyPair5, order);

        // collected two more signatures
        Cell signedOrder = MultisigWallet.addSignatures(order,
                List.of(
                        MultisigSignature.builder()
                                .pubKeyPosition(pubkey3Index)
                                .signature(orderSignatureUser3)
                                .build(),
                        MultisigSignature.builder()
                                .pubKeyPosition(pubkey4Index)
                                .signature(orderSignatureUser4)
                                .build(),
                        MultisigSignature.builder()
                                .pubKeyPosition(pubkey5Index)
                                .signature(orderSignatureUser5)
                                .build()
                )
        );

        signedOrder.toFile("signedOrder.boc", false);

        // submitter keypair must come from User3 or User4, otherwise you get error 34
        contract.sendOrder(tonlib, keyPair5, pubkey5Index, signedOrder);
        Utils.sleep(30, "processing 1st query");

        Pair<Long, Long> queryState = contract.getQueryState(tonlib, queryId);
        log.info("get_query_state (query {}): status {}, mask {}", queryId, queryState.getLeft(), queryState.getRight());

        assertThat(queryState.getLeft()).isEqualTo(-1);
    }

    /**
     * One user deploys a multisig wallet and send the first order,
     * other user then collects offline more signatures and sends them to the wallet.
     * Hybrid: On-chain/Off-chain consensus.
     */
    @Test
    public void testWalletMultisigHybrid() throws InterruptedException {

        log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
        log.info("pubKey2 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
        log.info("pubKey3 {}", Utils.bytesToHex(keyPair3.getPublicKey()));
        log.info("pubKey4 {}", Utils.bytesToHex(keyPair4.getPublicKey()));
        log.info("pubKey5 {}", Utils.bytesToHex(keyPair5.getPublicKey()));

        BigInteger queryId = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 2 * 60 * 60L, 32));

        Long walletId = new Random().nextLong() & 0xffffffffL;
        log.info("queryId {}, walletId {}", queryId, walletId);

        int rootIndex = 0;
        int pubkey2Index = 1;
        int pubkey3Index = 2;
        int pubkey4Index = 3;
        int pubkey5Index = 4;
        int k = 3;
        int n = 5;

        Options options = Options.builder()
                .publicKey(ownerKeyPair.getPublicKey())
                .walletId(walletId)
                .multisigConfig(MultisigConfig.builder()
                        .queryId(queryId)
                        .k(k)
                        .n(n)
                        .rootI(rootIndex)
                        .owners(
                                List.of(
                                        OwnerInfo.builder()
                                                .publicKey(ownerKeyPair.getPublicKey())
                                                .flood(1)
                                                .build(),
                                        OwnerInfo.builder()
                                                .publicKey(keyPair2.getPublicKey())
                                                .flood(2)
                                                .build(),
                                        OwnerInfo.builder()
                                                .publicKey(keyPair3.getPublicKey())
                                                .flood(3)
                                                .build(),
                                        OwnerInfo.builder()
                                                .publicKey(keyPair4.getPublicKey())
                                                .flood(4)
                                                .build(),
                                        OwnerInfo.builder()
                                                .publicKey(keyPair5.getPublicKey())
                                                .flood(5)
                                                .build()
                                )
                        ).build())
                .build();

        Wallet wallet = new Wallet(WalletVersion.multisig, options);
        MultisigWallet contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        Utils.sleep(30, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        MultisigWalletConfig config = MultisigWalletConfig.builder()
                .build();
        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying"); // with empty ext-msg

        log.info("owners publicKeysHex {}", contract.getPublicKeysHex(tonlib));

        Pair<Long, Long> n_k = contract.getNandK(tonlib);
        log.info("n {}, k {}", n_k.getLeft(), n_k.getRight());

        // You can include up to 3 destinations
        Cell msg1 = MultisigWallet.createOneInternalMsg(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);
        Cell msg2 = MultisigWallet.createOneInternalMsg(Address.of("EQDUna0j-TKlMU9pOBBHNoLzpwlewHl7S1qXtnaYdTTs_Ict"), Utils.toNano(0.6), 3);
        Cell msg3 = MultisigWallet.createOneInternalMsg(Address.of("EQCAy2ue54I-uDvEgD3qXdqjtrJI4F4OeFn3V10Kgt0jXpQn"), Utils.toNano(0.7), 3);

        // Having message(s) to send you can group it to a new order
        Cell order = MultisigWallet.createOrder(walletId, queryId, msg1, msg2, msg3);
        order.toFile("order.boc", false);

        byte[] orderSignatureUser1 = MultisigWallet.signOrder(ownerKeyPair, order);
        byte[] orderSignatureUser2 = MultisigWallet.signOrder(keyPair2, order);
        byte[] orderSignatureUser3 = MultisigWallet.signOrder(keyPair3, order);
        byte[] orderSignatureUser4 = MultisigWallet.signOrder(keyPair4, order);
        byte[] orderSignatureUser5 = MultisigWallet.signOrder(keyPair5, order);

        contract.sendOrder(tonlib, ownerKeyPair, rootIndex, order);
        Utils.sleep(30, "processing 1st query");

        Pair<Long, Long> queryState = contract.getQueryState(tonlib, queryId);
        log.info("get_query_state (query {}): status {}, mask {}", queryId, queryState.getLeft(), queryState.getRight());

        // collected two more signatures
        Cell signedOrder = MultisigWallet.addSignatures(order,
                List.of(
                        MultisigSignature.builder()
                                .pubKeyPosition(pubkey3Index)
                                .signature(orderSignatureUser3)
                                .build(),
                        MultisigSignature.builder()
                                .pubKeyPosition(pubkey4Index)
                                .signature(orderSignatureUser4)
                                .build()
                )
        );

        signedOrder.toFile("signedOrder.boc", false);

        // submitter keypair must come from User3 or User4, otherwise you get error 34
        contract.sendOrder(tonlib, keyPair3, pubkey3Index, signedOrder);
        Utils.sleep(20, "processing 1st query");

        showMessagesInfo(contract.getMessagesUnsigned(tonlib), "Messages-Unsigned");
        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, rootIndex), "Messages-SignedByIndex-" + rootIndex);
        showMessagesInfo(contract.getMessagesUnsignedByIndex(tonlib, rootIndex), "Messages-UnsignedByIndex-" + rootIndex);
        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, pubkey2Index), "Messages-SignedByIndex-" + pubkey2Index);
        showMessagesInfo(contract.getMessagesUnsignedByIndex(tonlib, pubkey2Index), "Messages-UnsignedByIndex-" + pubkey2Index);

        queryState = contract.getQueryState(tonlib, queryId);
        log.info("get_query_state (query {}): status {}, mask {}", queryId, queryState.getLeft(), queryState.getRight());

        assertThat(queryState.getLeft()).isEqualTo(-1);

        // 1 2 3
        Cell query = MultisigWallet.createQuery(ownerKeyPair,
                List.of(
                        MultisigSignature.builder()
                                .pubKeyPosition(rootIndex)
                                .signature(orderSignatureUser1)
                                .build(),
                        MultisigSignature.builder()
                                .pubKeyPosition(pubkey2Index)
                                .signature(orderSignatureUser2)
                                .build(),
                        MultisigSignature.builder()
                                .pubKeyPosition(pubkey3Index)
                                .signature(orderSignatureUser3)
                                .build()
                ), order);
        Pair<Long, Long> cnt_mask = contract.checkQuerySignatures(tonlib, query);
        log.info("cnt {}, mask {}", cnt_mask.getLeft(), cnt_mask.getRight());

        // 1 2 3 5
        query = MultisigWallet.createQuery(ownerKeyPair,
                List.of(
                        MultisigSignature.builder()
                                .pubKeyPosition(rootIndex)
                                .signature(orderSignatureUser1)
                                .build(),
                        MultisigSignature.builder()
                                .pubKeyPosition(pubkey2Index)
                                .signature(orderSignatureUser2)
                                .build(),
                        MultisigSignature.builder()
                                .pubKeyPosition(pubkey3Index)
                                .signature(orderSignatureUser3)
                                .build(),
                        MultisigSignature.builder()
                                .pubKeyPosition(pubkey4Index)
                                .signature(orderSignatureUser4)
                                .build(),
                        MultisigSignature.builder()
                                .pubKeyPosition(pubkey5Index)
                                .signature(orderSignatureUser5)
                                .build()
                ), order);

        cnt_mask = contract.checkQuerySignatures(tonlib, query);
        log.info("cnt {}, mask {}", cnt_mask.getLeft(), cnt_mask.getRight());
    }

    @Test
    public void testGetInitState() throws InterruptedException {

        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
//                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
        log.info("pubKey1 {}", Utils.bytesToHex(keyPair2.getPublicKey()));

        BigInteger queryId = BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60L << 32);

        Long walletId = new Random().nextLong() & 0xffffffffL;
        log.info("queryId {}, walletId {}", queryId.toString(10), walletId);

        int k = 1;
        int n = 2;

        Options options = Options.builder()
                .publicKey(ownerKeyPair.getPublicKey())
                .walletId(walletId)
                .multisigConfig(MultisigConfig.builder()
                        .queryId(queryId)
                        .k(k)
                        .n(n)
                        .rootI(0)
                        .owners(List.of(
                                OwnerInfo.builder()
                                        .publicKey(ownerKeyPair.getPublicKey())
                                        .flood(1)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair2.getPublicKey())
                                        .flood(2)
                                        .build()
                        ))
                        .build())
                .build();


        Wallet wallet = new Wallet(WalletVersion.multisig, options);
        MultisigWallet contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
        Utils.sleep(30, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        MultisigWalletConfig config = MultisigWalletConfig.builder()
                .build();
        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying"); // with empty ext msg

        List<OwnerInfo> ownersPublicKeys = List.of(
                OwnerInfo.builder()
                        .publicKey(ownerKeyPair.getPublicKey())
                        .flood(0)
                        .build(),
                OwnerInfo.builder()
                        .publicKey(keyPair2.getPublicKey())
                        .flood(1)
                        .build()
        );

        Cell stateInit = contract.getInitState(tonlib, walletId, n, k, contract.createOwnersInfosDict(ownersPublicKeys));
        log.info("state-init {}", stateInit.toHex(false));
    }

    /**
     * Test different root index and multiple orders. Consensus gets calculated on-chain.
     */
    @Test
    public void testRootIAndMultipleOrdersOnChain() throws InterruptedException {
        log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
        log.info("pubKey1 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
        log.info("pubKey2 {}", Utils.bytesToHex(keyPair3.getPublicKey()));

        BigInteger queryId1 = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32));
        BigInteger queryId2 = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32) - 5);

        Long walletId = new Random().nextLong() & 0xffffffffL;
        log.info("queryId-1 {}, walletId {}", queryId1.toString(10), walletId);
        log.info("queryId-2 {}, walletId {}", queryId2.toString(10), walletId);

        int k = 2;
        int n = 3;

        Options options = Options.builder()
                .publicKey(ownerKeyPair.getPublicKey())
                .walletId(walletId)
                .multisigConfig(MultisigConfig.builder()
                        .queryId(queryId1)
                        .k(k)
                        .n(n)
                        .rootI(2) // initial root index
                        .owners(List.of(
                                OwnerInfo.builder()
                                        .publicKey(ownerKeyPair.getPublicKey())
                                        .flood(1)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair2.getPublicKey())
                                        .flood(2)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair3.getPublicKey())
                                        .flood(3)
                                        .build()
                        ))
                        .build())
                .build();

        Wallet wallet = new Wallet(WalletVersion.multisig, options);
        MultisigWallet contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        Utils.sleep(30, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        MultisigWalletConfig config = MultisigWalletConfig.builder()
                .build();
        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying"); // with empty ext msg

        log.info("owners publicKeysHex {}", contract.getPublicKeysHex(tonlib));

        Pair<Long, Long> n_k = contract.getNandK(tonlib);
        log.info("n {}, k {}", n_k.getLeft(), n_k.getRight());

        Cell txMsg1 = MultisigWallet.createOneInternalMsg(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);
        Cell order1 = MultisigWallet.createOrder(walletId, queryId1, txMsg1);
        byte[] order1Signature3 = MultisigWallet.signCell(keyPair3, order1);

        // send order-1 signed by 3rd owner (root index 2)
        contract.sendOrder(tonlib, keyPair3, 2, order1); // root index 2
        Utils.sleep(30, "processing 1st query");

        Pair<Long, Long> queryState = contract.getQueryState(tonlib, queryId1);
        log.info("get_query_state (query {}): status {}, mask {}", queryId1.toString(10), queryState.getLeft(), queryState.getRight());

        Cell msg2 = MultisigWallet.createOneInternalMsg(Address.of("EQCAy2ue54I-uDvEgD3qXdqjtrJI4F4OeFn3V10Kgt0jXpQn"), Utils.toNano(0.8), 3);
        Cell order2 = MultisigWallet.createOrder(walletId, queryId2, msg2);

        // send order-2 signed by 2nd owner (root index 1)
        contract.sendOrder(tonlib, keyPair2, 1, order2);
        Utils.sleep(30, "processing 2nd query");

        queryState = contract.getQueryState(tonlib, queryId2);
        log.info("get_query_state (query {}): status {}, mask {}", queryId2, queryState.getLeft(), queryState.getRight());

        // send order-2 signed by 3rd owner (root index 2)
        contract.sendOrder(tonlib, keyPair3, 2, order2);
        Utils.sleep(30, "processing 3rd query");

        queryState = contract.getQueryState(tonlib, queryId2);
        log.info("get_query_state (query {}): status {}, mask {}", queryId2, queryState.getLeft(), queryState.getRight());
        assertThat(queryState.getLeft()).isEqualTo(-1);
    }

    /**
     * Each owner sends the extmsg signed by him, containing the order. Consensus gets calculated on-chain.
     */
    @Test
    public void testEmptySignaturesListOnChain() throws InterruptedException {
        log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
        log.info("pubKey1 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
        log.info("pubKey2 {}", Utils.bytesToHex(keyPair3.getPublicKey()));

        BigInteger queryId = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32));

        Long walletId = new Random().nextLong() & 0xffffffffL;
        log.info("queryId-1 {}, walletId {}", queryId.toString(10), walletId);

        int k = 2;
        int n = 3;

        Options options = Options.builder()
                .publicKey(ownerKeyPair.getPublicKey())
                .walletId(walletId)
                .multisigConfig(MultisigConfig.builder()
                        .queryId(queryId)
                        .k(k)
                        .n(n)
                        .rootI(0) // initial root index
                        .owners(List.of(
                                OwnerInfo.builder()
                                        .publicKey(ownerKeyPair.getPublicKey())
                                        .flood(1)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair2.getPublicKey())
                                        .flood(2)
                                        .build()
                        ))
                        .build())
                .build();


        Wallet wallet = new Wallet(WalletVersion.multisig, options);
        MultisigWallet contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
        Utils.sleep(30, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        MultisigWalletConfig config = MultisigWalletConfig.builder()
                .build();
        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying"); // with empty ext msg

        log.info("owners publicKeysHex {}", contract.getPublicKeysHex(tonlib));

        Pair<Long, Long> n_k = contract.getNandK(tonlib);
        log.info("n {}, k {}", n_k.getLeft(), n_k.getRight());

        Cell txMsg1 = MultisigWallet.createOneInternalMsg(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);
        Cell order = MultisigWallet.createOrder(walletId, queryId, txMsg1);

        // send order-1 signed by 1st owner (root index 0)
        contract.sendOrder(tonlib, ownerKeyPair.getSecretKey(), 0, order); // root index 0
        Utils.sleep(30, "processing 1st query");

        showMessagesInfo(contract.getMessagesUnsignedByIndex(tonlib, 0), "MessagesUnsignedByIndex-" + 0);
        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, 0), "MessagesSignedByIndex-" + 0);

        Pair<Long, Long> queryState = contract.getQueryState(tonlib, queryId);
        log.info("get_query_state (query {}): status {}, mask {}", queryId, queryState.getLeft(), queryState.getRight());

        // send order-1 signed by 2nd owner (root index 1)
        contract.sendOrder(tonlib, keyPair2.getSecretKey(), 1, order); // root index 1
        Utils.sleep(30, "processing 2st query");

        queryState = contract.getQueryState(tonlib, queryId);
        log.info("get_query_state (query {}): status {}, mask {}", queryId, queryState.getLeft(), queryState.getRight());
        assertThat(queryState.getLeft()).isEqualTo(-1);
    }

    @Test
    public void testMultisigPendingQueries() throws InterruptedException {
        log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
        log.info("pubKey2 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
        log.info("pubKey3 {}", Utils.bytesToHex(keyPair3.getPublicKey()));
        log.info("pubKey4 {}", Utils.bytesToHex(keyPair4.getPublicKey()));
        log.info("pubKey5 {}", Utils.bytesToHex(keyPair5.getPublicKey()));

        BigInteger queryId1 = BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60L << 32);
        BigInteger queryId2 = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32) - 5);

        Long walletId = new Random().nextLong() & 0xffffffffL;
        log.info("queryId-1 {}, walletId {}", queryId1.toString(10), walletId);
        log.info("queryId-2 {}, walletId {}", queryId2.toString(10), walletId);

        int rootIndex = 0;
        int pubkey3Index = 2;
        int k = 3;
        int n = 5;

        Cell msg1 = MultisigWallet.createOneInternalMsg(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.3), 3);
        Cell order1 = MultisigWallet.createOrder(walletId, queryId1, msg1);

        Cell msg2 = MultisigWallet.createOneInternalMsg(Address.of("EQDUna0j-TKlMU9pOBBHNoLzpwlewHl7S1qXtnaYdTTs_Ict"), Utils.toNano(0.4), 3);

        Options options = Options.builder()
                .publicKey(ownerKeyPair.getPublicKey())
                .walletId(walletId)
                .multisigConfig(MultisigConfig.builder()
                        .queryId(queryId1)
                        .k(k)
                        .n(n)
                        .rootI(rootIndex)
                        .owners(List.of(
                                OwnerInfo.builder()
                                        .publicKey(ownerKeyPair.getPublicKey())
                                        .flood(1)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair2.getPublicKey())
                                        .flood(2)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair3.getPublicKey())
                                        .flood(3)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair4.getPublicKey())
                                        .flood(4)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair5.getPublicKey())
                                        .flood(5)
                                        .build()
                        ))
                        .pendingQueries(List.of(
                                PendingQuery.builder()
                                        .queryId(queryId1)
                                        .creatorI(rootIndex)
                                        .cnt(2) // number of confirmation
                                        .cntBits(3) // bit mask of confirmed pubkeys
                                        .msg(msg1)
                                        .build(),
                                PendingQuery.builder()
                                        .queryId(queryId2)
                                        .creatorI(rootIndex)
                                        .cnt(1)
                                        .cntBits(1)
                                        .msg(msg2)
                                        .build()
                        ))
                        .build())
                .build();

        Wallet wallet = new Wallet(WalletVersion.multisig, options);
        MultisigWallet contract = wallet.create();


        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
        Utils.sleep(30, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        MultisigWalletConfig config = MultisigWalletConfig.builder()
                .build();
        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying"); // with empty ext msg

        log.info("owners publicKeysHex {}", contract.getPublicKeysHex(tonlib));

        Pair<Long, Long> queryState = contract.getQueryState(tonlib, queryId1);
        log.info("get_query_state (query-1 {}): status {}, mask {}", queryId1, queryState.getLeft(), queryState.getRight());

        queryState = contract.getQueryState(tonlib, queryId2);
        log.info("get_query_state (query-2 {}): status {}, mask {}", queryId2, queryState.getLeft(), queryState.getRight());


        showMessagesInfo(contract.getMessagesUnsigned(tonlib), "Messages-Unsigned");
        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, rootIndex), "Messages-SignedByIndex-" + rootIndex);
        showMessagesInfo(contract.getMessagesUnsignedByIndex(tonlib, rootIndex), "Messages-UnsignedByIndex-" + rootIndex);

        contract.sendOrder(tonlib, keyPair3.getSecretKey(), pubkey3Index, order1);

        Utils.sleep(30, "processing query");

        queryState = contract.getQueryState(tonlib, queryId1);
        log.info("get_query_state (query-1 {}): status {}, mask {}", queryId1, queryState.getLeft(), queryState.getRight());

        assertThat(queryState.getLeft()).isEqualTo(-1);

        showMessagesInfo(contract.getMessagesUnsigned(tonlib), "Messages-Unsigned");
        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, rootIndex), "Messages-SignedByIndex-" + rootIndex);
    }

    @Test
    public void testMergePendingQueries() throws InterruptedException {
        log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
        log.info("pubKey1 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
        log.info("pubKey2 {}", Utils.bytesToHex(keyPair3.getPublicKey()));

        BigInteger queryId1 = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32));
        BigInteger queryId2 = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32) - 5);

        Long walletId = new Random().nextLong() & 0xffffffffL;
        log.info("queryId-1 {}, walletId {}", queryId1.toString(10), walletId);

        int k = 2;
        int n = 3;

        Cell msg1 = MultisigWallet.createOneInternalMsg(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.3), 3);
        Cell msg2 = MultisigWallet.createOneInternalMsg(Address.of("EQDUna0j-TKlMU9pOBBHNoLzpwlewHl7S1qXtnaYdTTs_Ict"), Utils.toNano(0.4), 3);

        Options options = Options.builder()
                .publicKey(ownerKeyPair.getPublicKey())
                .walletId(walletId)
                .multisigConfig(MultisigConfig.builder()
                        .queryId(queryId1)
                        .k(k)
                        .n(n)
                        .rootI(0) // initial root index
                        .owners(List.of(
                                OwnerInfo.builder()
                                        .publicKey(ownerKeyPair.getPublicKey())
                                        .flood(1)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair2.getPublicKey())
                                        .flood(2)
                                        .build()
                        ))
                        .build())
                .build();


        Wallet wallet = new Wallet(WalletVersion.multisig, options);
        MultisigWallet contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
        Utils.sleep(30, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        MultisigWalletConfig config = MultisigWalletConfig.builder()
                .build();
        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying"); // with empty ext msg

        log.info("owners publicKeysHex {}", contract.getPublicKeysHex(tonlib));
        Cell dict1 = MultisigWallet.createPendingQueries(
                List.of(
                        PendingQuery.builder()
                                .queryId(queryId1)
                                .creatorI(0)
                                .cnt(2) // number of confirmation
                                .cntBits(3) // bit mask of confirmed pubkeys
                                .msg(msg1)
                                .build(),
                        PendingQuery.builder()
                                .queryId(queryId2)
                                .creatorI(0)
                                .cnt(2)
                                .cntBits(3)
                                .msg(msg1)
                                .build()
                ), n);

        Cell dict2 = MultisigWallet.createPendingQueries(
                List.of(
                        PendingQuery.builder()
                                .queryId(queryId1)
                                .creatorI(0)
                                .cnt(2)
                                .cntBits(3)
                                .msg(msg2)
                                .build(),
                        PendingQuery.builder()
                                .queryId(queryId2)
                                .creatorI(0)
                                .cnt(2)
                                .cntBits(3)
                                .msg(msg1)
                                .build()
                ), n);

        Cell mergeDict = contract.mergePendingQueries(tonlib, dict1, dict2);
        log.info("merged dict {}", mergeDict);
    }

    private void showMessagesInfo(Map<BigInteger, Cell> messages, String label) {
        if (messages.isEmpty()) {
            log.info("{} result is empty", label);
        }
        for (Map.Entry<BigInteger, Cell> entry : messages.entrySet()) {
            BigInteger query_id = entry.getKey();
            Cell query = entry.getValue();

            log.info("{} query-id {}, msg {}", label, query_id, query);
        }
    }
}
