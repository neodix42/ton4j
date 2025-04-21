package org.ton.java.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.multisig.MultiSigWallet;
import org.ton.java.smartcontract.types.MultiSigConfig;
import org.ton.java.smartcontract.types.MultisigSignature;
import org.ton.java.smartcontract.types.OwnerInfo;
import org.ton.java.smartcontract.types.PendingQuery;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
@Deprecated
public class TestWalletMultiSig extends CommonTest {

  TweetNaclFast.Signature.KeyPair ownerKeyPair = Utils.generateSignatureKeyPair();
  TweetNaclFast.Signature.KeyPair keyPair2 = Utils.generateSignatureKeyPair();
  TweetNaclFast.Signature.KeyPair keyPair3 = Utils.generateSignatureKeyPair();
  TweetNaclFast.Signature.KeyPair keyPair4 = Utils.generateSignatureKeyPair();
  TweetNaclFast.Signature.KeyPair keyPair5 = Utils.generateSignatureKeyPair();

  /**
   * Any user deploys a multiSig wallet. Any user from the list creates an order and gathers all the
   * required signatures, then sends the order to the wallet.
   */
  @Test
  public void testWalletMultiSigOffline() throws InterruptedException {

    log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
    log.info("pubKey2 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
    log.info("pubKey3 {}", Utils.bytesToHex(keyPair3.getPublicKey()));
    log.info("pubKey4 {}", Utils.bytesToHex(keyPair4.getPublicKey()));
    log.info("pubKey5 {}", Utils.bytesToHex(keyPair5.getPublicKey()));

    BigInteger queryId = new BigInteger("9223372036854775807");

    Long walletId = 1045609917L;
    log.info("queryId {}, walletId {}", queryId, walletId);

    int rootIndex = 0;
    int pubkey2Index = 1;
    int pubkey3Index = 2;
    int pubkey4Index = 3;
    int pubkey5Index = 4;
    int k = 3;
    int n = 5;

    MultiSigWallet contract =
        MultiSigWallet.builder()
            .tonlib(tonlib)
            .keyPair(ownerKeyPair)
            .walletId(walletId)
            .config(
                MultiSigConfig.builder()
                    .queryId(queryId)
                    .k(k)
                    .n(n)
                    .rootI(rootIndex)
                    .owners(
                        Arrays.asList(
                            OwnerInfo.builder()
                                .publicKey(ownerKeyPair.getPublicKey())
                                .flood(1)
                                .build(),
                            OwnerInfo.builder().publicKey(keyPair2.getPublicKey()).flood(2).build(),
                            OwnerInfo.builder().publicKey(keyPair3.getPublicKey()).flood(3).build(),
                            OwnerInfo.builder().publicKey(keyPair4.getPublicKey()).flood(4).build(),
                            OwnerInfo.builder()
                                .publicKey(keyPair5.getPublicKey())
                                .flood(5)
                                .build()))
                    .build())
            .build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(30); // with empty ext-msg

    log.info("owners publicKeys {}", contract.getPublicKeys());
    log.info("owners publicKeysHex {}", contract.getPublicKeysHex());

    Pair<Long, Long> n_k = contract.getNandK();
    log.info("n {}, k {}", n_k.getLeft(), n_k.getRight());

    // You can include up to 3 destinations
    Cell msg1 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);
    Cell msg2 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQDUna0j-TKlMU9pOBBHNoLzpwlewHl7S1qXtnaYdTTs_Ict"), Utils.toNano(0.6), 3);
    Cell msg3 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQCAy2ue54I-uDvEgD3qXdqjtrJI4F4OeFn3V10Kgt0jXpQn"), Utils.toNano(0.7), 3);

    // Having message(s) to send you can group it to a new order
    Cell order = MultiSigWallet.createOrder(walletId, queryId, msg1, msg2, msg3);
    order.toFile("order.boc");

    byte[] orderSignatureUser1 = MultiSigWallet.signOrder(ownerKeyPair, order);
    byte[] orderSignatureUser2 = MultiSigWallet.signOrder(keyPair2, order);
    byte[] orderSignatureUser3 = MultiSigWallet.signOrder(keyPair3, order);
    byte[] orderSignatureUser4 = MultiSigWallet.signOrder(keyPair4, order);
    byte[] orderSignatureUser5 = MultiSigWallet.signOrder(keyPair5, order);

    // collected two more signatures
    Cell signedOrder =
        MultiSigWallet.addSignatures(
            order,
            Arrays.asList(
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
                    .build()));

    signedOrder.toFile("signedOrder.boc", false);

    // submitter keypair must come from User3 or User4, otherwise you get error 34
    extMessageInfo = contract.sendOrder(keyPair5, pubkey5Index, signedOrder);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForBalanceChange(30);

    Pair<Long, Long> queryState = contract.getQueryState(queryId);
    log.info(
        "get_query_state (query {}): status {}, mask {}",
        queryId,
        queryState.getLeft(),
        queryState.getRight());

    assertThat(queryState.getLeft()).isEqualTo(-1);
  }

  /**
   * One user deploys a multisig wallet and send the first order, other user then collects offline
   * more signatures and sends them to the wallet. Hybrid: On-chain/Off-chain consensus.
   */
  @Test
  public void testWalletMultiSigHybrid() throws InterruptedException {

    log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
    log.info("pubKey2 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
    log.info("pubKey3 {}", Utils.bytesToHex(keyPair3.getPublicKey()));
    log.info("pubKey4 {}", Utils.bytesToHex(keyPair4.getPublicKey()));
    log.info("pubKey5 {}", Utils.bytesToHex(keyPair5.getPublicKey()));

    BigInteger queryId = BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60 * 60L << 32);

    Long walletId = new Random().nextLong() & 0xffffffffL;
    log.info("queryId {}, walletId {}", queryId, walletId);

    int rootIndex = 0;
    int pubkey2Index = 1;
    int pubkey3Index = 2;
    int pubkey4Index = 3;
    int pubkey5Index = 4;
    int k = 3;
    int n = 5;

    MultiSigWallet contract =
        MultiSigWallet.builder()
            .tonlib(tonlib)
            .keyPair(ownerKeyPair)
            .walletId(walletId)
            .config(
                MultiSigConfig.builder()
                    .queryId(queryId)
                    .k(k)
                    .n(n)
                    .rootI(rootIndex)
                    .owners(
                        Arrays.asList(
                            OwnerInfo.builder()
                                .publicKey(ownerKeyPair.getPublicKey())
                                .flood(1)
                                .build(),
                            OwnerInfo.builder().publicKey(keyPair2.getPublicKey()).flood(2).build(),
                            OwnerInfo.builder().publicKey(keyPair3.getPublicKey()).flood(3).build(),
                            OwnerInfo.builder().publicKey(keyPair4.getPublicKey()).flood(4).build(),
                            OwnerInfo.builder()
                                .publicKey(keyPair5.getPublicKey())
                                .flood(5)
                                .build()))
                    .build())
            .build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(45); // with empty ext-msg

    log.info("owners publicKeysHex {}", contract.getPublicKeysHex());

    Pair<Long, Long> n_k = contract.getNandK();
    log.info("n {}, k {}", n_k.getLeft(), n_k.getRight());

    // You can include up to 3 destinations
    Cell msg1 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);
    Cell msg2 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQDUna0j-TKlMU9pOBBHNoLzpwlewHl7S1qXtnaYdTTs_Ict"), Utils.toNano(0.6), 3);
    Cell msg3 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQCAy2ue54I-uDvEgD3qXdqjtrJI4F4OeFn3V10Kgt0jXpQn"), Utils.toNano(0.7), 3);

    // Having message(s) to send you can group it to a new order
    Cell order = MultiSigWallet.createOrder(walletId, queryId, msg1, msg2, msg3);
    order.toFile("order.boc", false);

    byte[] orderSignatureUser1 = MultiSigWallet.signOrder(ownerKeyPair, order);
    byte[] orderSignatureUser2 = MultiSigWallet.signOrder(keyPair2, order);
    byte[] orderSignatureUser3 = MultiSigWallet.signOrder(keyPair3, order);
    byte[] orderSignatureUser4 = MultiSigWallet.signOrder(keyPair4, order);
    byte[] orderSignatureUser5 = MultiSigWallet.signOrder(keyPair5, order);

    extMessageInfo = contract.sendOrder(ownerKeyPair, rootIndex, order);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    //        Utils.sleep(30, "processing 1st query");
    contract.waitForBalanceChange(30);

    Pair<Long, Long> queryState = contract.getQueryState(queryId);
    log.info(
        "get_query_state (query {}): status {}, mask {}",
        queryId,
        queryState.getLeft(),
        queryState.getRight());

    // collected two more signatures
    Cell signedOrder =
        MultiSigWallet.addSignatures(
            order,
            Arrays.asList(
                MultisigSignature.builder()
                    .pubKeyPosition(pubkey3Index)
                    .signature(orderSignatureUser3)
                    .build(),
                MultisigSignature.builder()
                    .pubKeyPosition(pubkey4Index)
                    .signature(orderSignatureUser4)
                    .build()));

    signedOrder.toFile("signedOrder.boc", false);

    // submitter keypair must come from User3 or User4, otherwise you get error 34
    extMessageInfo = contract.sendOrder(keyPair3, pubkey3Index, signedOrder);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    //        Utils.sleep(20, "processing 1st query");
    contract.waitForBalanceChange(30);

    showMessagesInfo(contract.getMessagesUnsigned(), "Messages-Unsigned");
    showMessagesInfo(
        contract.getMessagesSignedByIndex(rootIndex), "Messages-SignedByIndex-" + rootIndex);
    showMessagesInfo(
        contract.getMessagesUnsignedByIndex(rootIndex), "Messages-UnsignedByIndex-" + rootIndex);
    showMessagesInfo(
        contract.getMessagesSignedByIndex(pubkey2Index), "Messages-SignedByIndex-" + pubkey2Index);
    showMessagesInfo(
        contract.getMessagesUnsignedByIndex(pubkey2Index),
        "Messages-UnsignedByIndex-" + pubkey2Index);

    queryState = contract.getQueryState(queryId);
    log.info(
        "get_query_state (query {}): status {}, mask {}",
        queryId,
        queryState.getLeft(),
        queryState.getRight());

    assertThat(queryState.getLeft()).isEqualTo(-1);

    // 1 2 3
    Cell query =
        MultiSigWallet.createQuery(
            ownerKeyPair,
            Arrays.asList(
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
                    .build()),
            order);
    Pair<Long, Long> cnt_mask = contract.checkQuerySignatures(tonlib, query);
    log.info("cnt {}, mask {}", cnt_mask.getLeft(), cnt_mask.getRight());

    // 1 2 3 5
    query =
        MultiSigWallet.createQuery(
            ownerKeyPair,
            Arrays.asList(
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
                    .build()),
            order);

    cnt_mask = contract.checkQuerySignatures(tonlib, query);
    log.info("cnt {}, mask {}", cnt_mask.getLeft(), cnt_mask.getRight());
  }

  @Test
  public void testGetInitState() throws InterruptedException {

    log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
    log.info("pubKey1 {}", Utils.bytesToHex(keyPair2.getPublicKey()));

    BigInteger queryId = BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60L << 32);

    Long walletId = new Random().nextLong() & 0xffffffffL;
    log.info("queryId {}, walletId {}", queryId.toString(10), walletId);

    int k = 1;
    int n = 2;

    MultiSigWallet contract =
        MultiSigWallet.builder()
            .tonlib(tonlib)
            .keyPair(ownerKeyPair)
            .walletId(walletId)
            .config(
                MultiSigConfig.builder()
                    .queryId(queryId)
                    .k(k)
                    .n(n)
                    .rootI(0)
                    .owners(
                        Arrays.asList(
                            OwnerInfo.builder()
                                .publicKey(ownerKeyPair.getPublicKey())
                                .flood(1)
                                .build(),
                            OwnerInfo.builder()
                                .publicKey(keyPair2.getPublicKey())
                                .flood(2)
                                .build()))
                    .build())
            .build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(45); // with empty ext msg

    List<OwnerInfo> ownersPublicKeys =
        Arrays.asList(
            OwnerInfo.builder().publicKey(ownerKeyPair.getPublicKey()).flood(0).build(),
            OwnerInfo.builder().publicKey(keyPair2.getPublicKey()).flood(1).build());

    Cell stateInit =
        contract.getInitState(walletId, n, k, contract.createOwnersInfoDict(ownersPublicKeys));
    log.info("state-init {}", stateInit.toHex(false));
  }

  /** Test different root index and multiple orders. Consensus gets calculated on-chain. */
  @Test
  public void testRootIAndMultipleOrdersOnChain() throws InterruptedException {
    log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
    log.info("pubKey1 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
    log.info("pubKey2 {}", Utils.bytesToHex(keyPair3.getPublicKey()));

    BigInteger queryId1 =
        BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32));
    BigInteger queryId2 =
        BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32) - 5);

    Long walletId = new Random().nextLong() & 0xffffffffL;
    log.info("queryId-1 {}, walletId {}", queryId1.toString(10), walletId);
    log.info("queryId-2 {}, walletId {}", queryId2.toString(10), walletId);

    int k = 2;
    int n = 3;

    MultiSigWallet contract =
        MultiSigWallet.builder()
            .tonlib(tonlib)
            .keyPair(ownerKeyPair)
            .walletId(walletId)
            .config(
                MultiSigConfig.builder()
                    .queryId(queryId1)
                    .k(k)
                    .n(n)
                    .rootI(2) // initial root index
                    .owners(
                        Arrays.asList(
                            OwnerInfo.builder()
                                .publicKey(ownerKeyPair.getPublicKey())
                                .flood(1)
                                .build(),
                            OwnerInfo.builder().publicKey(keyPair2.getPublicKey()).flood(2).build(),
                            OwnerInfo.builder()
                                .publicKey(keyPair3.getPublicKey())
                                .flood(3)
                                .build()))
                    .build())
            .build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(45); // with empty ext msg

    log.info("owners publicKeysHex {}", contract.getPublicKeysHex());

    Pair<Long, Long> n_k = contract.getNandK();
    log.info("n {}, k {}", n_k.getLeft(), n_k.getRight());

    Cell txMsg1 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);
    Cell order1 = MultiSigWallet.createOrder(walletId, queryId1, txMsg1);
    byte[] order1Signature3 = MultiSigWallet.signCell(keyPair3, order1);

    // send order-1 signed by 3rd owner (root index 2)
    extMessageInfo = contract.sendOrder(keyPair3, 2, order1); // root index 2
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForBalanceChange(30);

    Pair<Long, Long> queryState = contract.getQueryState(queryId1);
    log.info(
        "get_query_state (query {}): status {}, mask {}",
        queryId1.toString(10),
        queryState.getLeft(),
        queryState.getRight());

    Cell msg2 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQCAy2ue54I-uDvEgD3qXdqjtrJI4F4OeFn3V10Kgt0jXpQn"), Utils.toNano(0.8), 3);
    Cell order2 = MultiSigWallet.createOrder(walletId, queryId2, msg2);

    // send order-2 signed by 2nd owner (root index 1)
    extMessageInfo = contract.sendOrder(keyPair2, 1, order2);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForBalanceChange(30);

    queryState = contract.getQueryState(queryId2);
    log.info(
        "get_query_state (query {}): status {}, mask {}",
        queryId2,
        queryState.getLeft(),
        queryState.getRight());

    // send order-2 signed by 3rd owner (root index 2)
    extMessageInfo = contract.sendOrder(keyPair3, 2, order2);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForBalanceChange(30);

    queryState = contract.getQueryState(queryId2);
    log.info(
        "get_query_state (query {}): status {}, mask {}",
        queryId2,
        queryState.getLeft(),
        queryState.getRight());
    assertThat(queryState.getLeft()).isEqualTo(-1);
  }

  /**
   * Each owner sends the extmsg signed by him, containing the order. Consensus gets calculated
   * on-chain.
   */
  @Test
  public void testEmptySignaturesListOnChain() throws InterruptedException {

    log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
    log.info("pubKey1 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
    log.info("pubKey2 {}", Utils.bytesToHex(keyPair3.getPublicKey()));

    BigInteger queryId =
        BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32));

    Long walletId = new Random().nextLong() & 0xffffffffL;
    log.info("queryId-1 {}, walletId {}", queryId.toString(10), walletId);

    int k = 2;
    int n = 3;

    MultiSigWallet contract =
        MultiSigWallet.builder()
            .tonlib(tonlib)
            .keyPair(ownerKeyPair)
            .walletId(walletId)
            .config(
                MultiSigConfig.builder()
                    .queryId(queryId)
                    .k(k)
                    .n(n)
                    .rootI(0) // initial root index
                    .owners(
                        Arrays.asList(
                            OwnerInfo.builder()
                                .publicKey(ownerKeyPair.getPublicKey())
                                .flood(1)
                                .build(),
                            OwnerInfo.builder()
                                .publicKey(keyPair2.getPublicKey())
                                .flood(2)
                                .build()))
                    .build())
            .build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(45); // with empty ext msg

    log.info("owners publicKeysHex {}", contract.getPublicKeysHex());

    Pair<Long, Long> n_k = contract.getNandK();
    log.info("n {}, k {}", n_k.getLeft(), n_k.getRight());

    Cell txMsg1 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);
    Cell order = MultiSigWallet.createOrder(walletId, queryId, txMsg1);

    // send order-1 signed by 1st owner (root index 0)
    extMessageInfo = contract.sendOrder(ownerKeyPair.getSecretKey(), 0, order); // root index 0
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForBalanceChange(30);

    showMessagesInfo(contract.getMessagesUnsignedByIndex(0), "MessagesUnsignedByIndex-" + 0);
    showMessagesInfo(contract.getMessagesSignedByIndex(0), "MessagesSignedByIndex-" + 0);

    Pair<Long, Long> queryState = contract.getQueryState(queryId);
    log.info(
        "get_query_state (query {}): status {}, mask {}",
        queryId,
        queryState.getLeft(),
        queryState.getRight());

    // send order-1 signed by 2nd owner (root index 1)
    extMessageInfo = contract.sendOrder(keyPair2.getSecretKey(), 1, order); // root index 1
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForBalanceChange(30);

    queryState = contract.getQueryState(queryId);
    log.info(
        "get_query_state (query {}): status {}, mask {}",
        queryId,
        queryState.getLeft(),
        queryState.getRight());
    assertThat(queryState.getLeft()).isEqualTo(-1);
  }

  @Test
  public void testMultiSigPendingQueries() throws InterruptedException {
    log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
    log.info("pubKey2 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
    log.info("pubKey3 {}", Utils.bytesToHex(keyPair3.getPublicKey()));
    log.info("pubKey4 {}", Utils.bytesToHex(keyPair4.getPublicKey()));
    log.info("pubKey5 {}", Utils.bytesToHex(keyPair5.getPublicKey()));

    BigInteger queryId1 = BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60L << 32);
    BigInteger queryId2 =
        BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32) - 5);

    Long walletId = new Random().nextLong() & 0xffffffffL;
    log.info("queryId-1 {}, walletId {}", queryId1.toString(10), walletId);
    log.info("queryId-2 {}, walletId {}", queryId2.toString(10), walletId);

    int rootIndex = 0;
    int pubkey3Index = 2;
    int k = 3;
    int n = 5;

    Cell msg1 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.3), 3);
    Cell order1 = MultiSigWallet.createOrder(walletId, queryId1, msg1);

    Cell msg2 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQDUna0j-TKlMU9pOBBHNoLzpwlewHl7S1qXtnaYdTTs_Ict"), Utils.toNano(0.4), 3);

    MultiSigWallet contract =
        MultiSigWallet.builder()
            .tonlib(tonlib)
            .keyPair(ownerKeyPair)
            .walletId(walletId)
            .config(
                MultiSigConfig.builder()
                    .queryId(queryId1)
                    .k(k)
                    .n(n)
                    .rootI(rootIndex)
                    .owners(
                        Arrays.asList(
                            OwnerInfo.builder()
                                .publicKey(ownerKeyPair.getPublicKey())
                                .flood(1)
                                .build(),
                            OwnerInfo.builder().publicKey(keyPair2.getPublicKey()).flood(2).build(),
                            OwnerInfo.builder().publicKey(keyPair3.getPublicKey()).flood(3).build(),
                            OwnerInfo.builder().publicKey(keyPair4.getPublicKey()).flood(4).build(),
                            OwnerInfo.builder()
                                .publicKey(keyPair5.getPublicKey())
                                .flood(5)
                                .build()))
                    .pendingQueries(
                        Arrays.asList(
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
                                .build()))
                    .build())
            .build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(45); // with empty ext msg

    log.info("owners publicKeysHex {}", contract.getPublicKeysHex());

    Pair<Long, Long> queryState = contract.getQueryState(queryId1);
    log.info(
        "get_query_state (query-1 {}): status {}, mask {}",
        queryId1,
        queryState.getLeft(),
        queryState.getRight());

    queryState = contract.getQueryState(queryId2);
    log.info(
        "get_query_state (query-2 {}): status {}, mask {}",
        queryId2,
        queryState.getLeft(),
        queryState.getRight());

    showMessagesInfo(contract.getMessagesUnsigned(), "Messages-Unsigned");
    showMessagesInfo(
        contract.getMessagesSignedByIndex(rootIndex), "Messages-SignedByIndex-" + rootIndex);
    showMessagesInfo(
        contract.getMessagesUnsignedByIndex(rootIndex), "Messages-UnsignedByIndex-" + rootIndex);

    extMessageInfo = contract.sendOrder(keyPair3.getSecretKey(), pubkey3Index, order1);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    //        Utils.sleep(30, "processing query");
    contract.waitForBalanceChange(30);

    queryState = contract.getQueryState(queryId1);
    log.info(
        "get_query_state (query-1 {}): status {}, mask {}",
        queryId1,
        queryState.getLeft(),
        queryState.getRight());

    assertThat(queryState.getLeft()).isEqualTo(-1);

    showMessagesInfo(contract.getMessagesUnsigned(), "Messages-Unsigned");
    showMessagesInfo(
        contract.getMessagesSignedByIndex(rootIndex), "Messages-SignedByIndex-" + rootIndex);
  }

  @Test
  public void testMergePendingQueries() throws InterruptedException {
    log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
    log.info("pubKey1 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
    log.info("pubKey2 {}", Utils.bytesToHex(keyPair3.getPublicKey()));

    BigInteger queryId1 =
        BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32));
    BigInteger queryId2 =
        BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 10 * 60L, 32) - 5);

    long walletId = new Random().nextLong() & 0xffffffffL;
    log.info("queryId-1 {}, walletId {}", queryId1.toString(10), walletId);

    int k = 2;
    int n = 3;

    Cell msg1 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.3), 3);
    Cell msg2 =
        MultiSigWallet.createOneInternalMsg(
            Address.of("EQDUna0j-TKlMU9pOBBHNoLzpwlewHl7S1qXtnaYdTTs_Ict"), Utils.toNano(0.4), 3);

    MultiSigWallet contract =
        MultiSigWallet.builder()
            .tonlib(tonlib)
            .keyPair(ownerKeyPair)
            .walletId(walletId)
            .config(
                MultiSigConfig.builder()
                    .queryId(queryId1)
                    .k(k)
                    .n(n)
                    .rootI(0) // initial root index
                    .owners(
                        Arrays.asList(
                            OwnerInfo.builder()
                                .publicKey(ownerKeyPair.getPublicKey())
                                .flood(1)
                                .build(),
                            OwnerInfo.builder()
                                .publicKey(keyPair2.getPublicKey())
                                .flood(2)
                                .build()))
                    .build())
            .build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(45); // with empty ext msg

    log.info("owners publicKeysHex {}", contract.getPublicKeysHex());
    Cell dict1 =
        MultiSigWallet.createPendingQueries(
            Arrays.asList(
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
                    .build()),
            n);

    Cell dict2 =
        MultiSigWallet.createPendingQueries(
            Arrays.asList(
                PendingQuery.builder()
                    .queryId(queryId1)
                    .creatorI(0)
                    .cnt(2)
                    .cntBits(3)
                    .msg(msg1)
                    .build(),
                PendingQuery.builder()
                    .queryId(queryId2)
                    .creatorI(0)
                    .cnt(2)
                    .cntBits(3)
                    .msg(msg1)
                    .build()),
            n);

    log.info("pendingQueriesToMerge1 {}", dict1.toHex(false));
    log.info("pendingQueriesToMerge2 {}", dict2.toHex(false));
    log.info("pendingQueriesToMerge1-hash {}", Utils.bytesToHex(dict1.getHash()));
    log.info("pendingQueriesToMerge2-hash {}", Utils.bytesToHex(dict2.getHash()));

    Cell mergeDict = contract.mergePendingQueries(tonlib, dict1, dict2);
    log.info("merged dict {}", mergeDict);
    assertThat(mergeDict).isNotNull();
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

  @Test
  public void testCellSerialization5() {

    CellBuilder cell = CellBuilder.beginCell();

    cell.storeUint(42, 32);
    cell.storeUint(5, 8);
    cell.storeUint(3, 8);
    cell.storeUint(0, 64);
    cell.storeDict(
        createOwnersInfoDict(
            Arrays.asList(
                OwnerInfo.builder().publicKey(ownerKeyPair.getPublicKey()).flood(1).build(),
                OwnerInfo.builder().publicKey(keyPair2.getPublicKey()).flood(2).build(),
                OwnerInfo.builder().publicKey(keyPair3.getPublicKey()).flood(3).build(),
                OwnerInfo.builder().publicKey(keyPair4.getPublicKey()).flood(4).build(),
                OwnerInfo.builder().publicKey(keyPair5.getPublicKey()).flood(5).build())));
    cell.storeBit(false); // initial  pending queries dict

    System.out.println("print cell: " + cell.endCell().print());

    String bocHexWithCrc = cell.endCell().toHex();
    System.out.println("print (bocHexWithCrc): " + bocHexWithCrc);

    Cell c = Cell.fromBoc(bocHexWithCrc);
    System.out.println("print c: \n" + c.print());
  }

  private Cell createOwnersInfoDict(List<OwnerInfo> testOwnerInfos) {
    int dictKeySize = 8;
    TonHashMapE dictDestinations = new TonHashMapE(dictKeySize);

    long i = 0; // key, index 16bit
    for (OwnerInfo testOwnerInfo : testOwnerInfos) {

      CellBuilder ownerInfoCell = CellBuilder.beginCell();
      ownerInfoCell.storeBytes(testOwnerInfo.getPublicKey()); // 256 bits
      ownerInfoCell.storeUint(testOwnerInfo.getFlood(), 8);

      dictDestinations.elements.put(
          i++, // key - index
          ownerInfoCell.endCell() // value - cell - OwnerInfo
          );
    }

    Cell cellDict =
        dictDestinations.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
            v -> (Cell) v);

    return cellDict;
  }
}
