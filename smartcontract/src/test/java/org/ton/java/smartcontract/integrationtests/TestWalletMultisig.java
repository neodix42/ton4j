package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.multisig.MultisigWallet;
import org.ton.java.smartcontract.types.MultisigConfig;
import org.ton.java.smartcontract.types.OwnerInfo;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletMultisig {

    TweetNaclFast.Signature.KeyPair ownerKeyPair = Utils.generateSignatureKeyPair();
    TweetNaclFast.Signature.KeyPair keyPair2 = Utils.generateSignatureKeyPair();
    TweetNaclFast.Signature.KeyPair keyPair3 = Utils.generateSignatureKeyPair();
    TweetNaclFast.Signature.KeyPair keyPair4 = Utils.generateSignatureKeyPair();
    TweetNaclFast.Signature.KeyPair keyPair5 = Utils.generateSignatureKeyPair();

    @Test
    public void testWalletMultisig() throws InterruptedException {

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                //   .verbosityLevel(VerbosityLevel.DEBUG)
                .build();


        log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
        log.info("pubKey2 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
        log.info("pubKey3 {}", Utils.bytesToHex(keyPair3.getPublicKey()));
        log.info("pubKey4 {}", Utils.bytesToHex(keyPair4.getPublicKey()));
        log.info("pubKey5 {}", Utils.bytesToHex(keyPair5.getPublicKey()));

        BigInteger queryId = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 5 * 60L, 32));

        Long walletId = new Random().nextLong() & 0xffffffffL;
        log.info("queryId {}, walletId {}", queryId.toString(10), walletId);

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
                        // todo initial pending query list
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
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        contract.deploy(tonlib, ownerKeyPair.getSecretKey());

        Utils.sleep(30, "deploying"); // with empty ext msg

        log.info("owners publicKeys {}", contract.getPublicKeys(tonlib));
        log.info("owners publicKeysHex {}", contract.getPublicKeysHex(tonlib));

        Pair<Long, Long> n_k = contract.getNandK(tonlib);
        log.info("n {}, k {}", n_k.getLeft(), n_k.getRight());

        // You can include up to 3 destinations
        Cell msg1 = contract.createOneInternalMsg(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);
        Cell msg2 = contract.createOneInternalMsg(Address.of("EQDUna0j-TKlMU9pOBBHNoLzpwlewHl7S1qXtnaYdTTs_Ict"), Utils.toNano(0.6), 3);
        Cell msg3 = contract.createOneInternalMsg(Address.of("EQCAy2ue54I-uDvEgD3qXdqjtrJI4F4OeFn3V10Kgt0jXpQn"), Utils.toNano(0.7), 3);

        // Having message(s) to send you can group it to a new order
        Cell order = contract.createOrder(msg1, msg2, msg3);

        byte[] orderSignature1 = contract.signCell(ownerKeyPair, order);
        byte[] orderSignature2 = contract.signCell(keyPair2, order);
        byte[] orderSignature3 = contract.signCell(keyPair3, order);
        byte[] orderSignature4 = contract.signCell(keyPair4, order);
        byte[] orderSignature5 = contract.signCell(keyPair5, order);

        // send order (request for transaction protected with k of n pubkeys) signed by first owner
        contract.sendOrder(tonlib, ownerKeyPair, rootIndex, order, queryId, List.of(orderSignature1));
        Utils.sleep(15, "processing 1st query");

        showMessagesInfo(contract.getMessagesUnsigned(tonlib), k, "MessagesUnsigned");
        //MessagesUnsigned query-id 9223372036854775807, creator-i 0, cnt 2, cnt-bits 0, msg 00010818181C_

        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, rootIndex), k, "MessagesSignedByIndex-" + rootIndex);
        //MessagesSignedByIndex-0 query-id 9223372036854775807, creator-i 0, cnt 2, cnt-bits 0, msg 00010818181C_

        showMessagesInfo(contract.getMessagesUnsignedByIndex(tonlib, rootIndex), k, "MessagesUnsignedByIndex-" + rootIndex);
        //MessagesUnsignedByIndex-0 result is empty

        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, pubkey2Index), k, "MessagesSignedByIndex-" + pubkey2Index);
        //MessagesSignedByIndex-1 result is empty

        showMessagesInfo(contract.getMessagesUnsignedByIndex(tonlib, pubkey2Index), k, "MessagesUnsignedByIndex-" + pubkey2Index);
        //MessagesUnsignedByIndex-1 query-id 9223372036854775807, creator-i 0, cnt 2, cnt-bits 0, msg 00010818181C_

        Pair<Long, Long> queryState = contract.getQueryState(tonlib, queryId);
        log.info("get_query_state (query {}): status {}, mask {}", queryId.toString(10), queryState.getLeft(), queryState.getRight());
        // get_query_state (query 9223372036854775807): status 0 cnt_bits? 1

        // 1 2 3
        Cell query = contract.createQuery(ownerKeyPair, List.of(orderSignature1, orderSignature2, orderSignature3), order);
        Pair<Long, Long> cnt_mask = contract.checkQuerySignatures(tonlib, query);
        log.info("cnt {}, mask {}", cnt_mask.getLeft(), cnt_mask.getRight());

        // 1 2 3 5
        query = contract.createQuery(ownerKeyPair, List.of(orderSignature1, orderSignature2, orderSignature3, orderSignature4, orderSignature5), order);
        cnt_mask = contract.checkQuerySignatures(tonlib, query);
        log.info("cnt {}, mask {}", cnt_mask.getLeft(), cnt_mask.getRight());

        // send order (request for transaction protected with k of n pubkeys) signed by third owner
        contract.sendOrder(tonlib, keyPair3, pubkey3Index, order, queryId, List.of(orderSignature3));
        Utils.sleep(15, "processing 2nd query");

        showMessagesInfo(contract.getMessagesUnsigned(tonlib), k, "MessagesUnsigned");
        //MessagesUnsigned query-id 9223372036854775807, creator-i 0, cnt 4, cnt-bits 1, msg 00021818181C_

        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, rootIndex), k, "MessagesSignedByIndex-" + rootIndex);
        //MessagesSignedByIndex-0 query-id 9223372036854775807, creator-i 0, cnt 4, cnt-bits 1, msg 00021818181C_

        showMessagesInfo(contract.getMessagesUnsignedByIndex(tonlib, rootIndex), k, "MessagesUnsignedByIndex-" + rootIndex);
        //MessagesUnsignedByIndex-0 result is empty

        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, pubkey2Index), k, "MessagesSignedByIndex-" + pubkey2Index);
        //MessagesSignedByIndex-1 query-id 9223372036854775807, creator-i 0, cnt 4, cnt-bits 1, msg 00021818181C_

        showMessagesInfo(contract.getMessagesUnsignedByIndex(tonlib, pubkey2Index), k, "MessagesUnsignedByIndex-" + pubkey2Index);
        //MessagesUnsignedByIndex-1 result is empty

        queryState = contract.getQueryState(tonlib, queryId);
        log.info("get_query_state (query {}): status {}, mask {}", queryId.toString(10), queryState.getLeft(), queryState.getRight());
        // get_query_state (query 9223372036854775807): status 0 cnt_bits? 5

        // send order (request for transaction protected with k of n pubkeys) signed by fifth owner
        contract.sendOrder(tonlib, keyPair5, pubkey5Index, order, queryId, List.of(orderSignature5));
        Utils.sleep(20, "processing 3nd query");

        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, rootIndex), k, "MessagesSignedByIndex-" + rootIndex);
        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, rootIndex), k, "MessagesSignedByIndex-" + pubkey2Index);
        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, rootIndex), k, "MessagesSignedByIndex-" + pubkey3Index);
        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, rootIndex), k, "MessagesSignedByIndex-" + pubkey4Index);
        showMessagesInfo(contract.getMessagesSignedByIndex(tonlib, rootIndex), k, "MessagesSignedByIndex-" + pubkey5Index);

        queryState = contract.getQueryState(tonlib, queryId);
        log.info("get_query_state (query {}): status {}, mask {}", queryId.toString(10), queryState.getLeft(), queryState.getRight());
        // get_query_state (query 9223372036854775807): status 0 cnt_bits? 21

        log.info("processed query? ({}) - {}", queryId, contract.processed(tonlib, queryId));
    }

    @Test
    public void testGetInitState() throws InterruptedException {

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
        log.info("pubKey1 {}", Utils.bytesToHex(keyPair2.getPublicKey()));

        BigInteger queryId = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 5 * 60L, 32));

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
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        contract.deploy(tonlib, ownerKeyPair.getSecretKey());

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

    @Test
    public void testRootIAndMultipleOrders() throws InterruptedException {

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

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
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        contract.deploy(tonlib, keyPair3.getSecretKey());

        Utils.sleep(30, "deploying"); // with empty ext msg

        log.info("owners publicKeysHex {}", contract.getPublicKeysHex(tonlib));

        Pair<Long, Long> n_k = contract.getNandK(tonlib);
        log.info("n {}, k {}", n_k.getLeft(), n_k.getRight());

        Cell txMsg1 = contract.createOneInternalMsg(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);
        Cell order1 = contract.createOrder(txMsg1);
        byte[] order1Signature3 = contract.signCell(keyPair3, order1);

        // send order-1 signed by 3rd owner (root index 2)
        contract.sendOrder(tonlib, keyPair3, 2, order1, queryId1, List.of(order1Signature3)); // root index 2
        Utils.sleep(15, "processing 1st query");

        Pair<Long, Long> queryState = contract.getQueryState(tonlib, queryId1);
        log.info("get_query_state (query {}): status {}, mask {}", queryId1.toString(10), queryState.getLeft(), queryState.getRight());

        Cell txMsg2 = contract.createOneInternalMsg(Address.of("EQCAy2ue54I-uDvEgD3qXdqjtrJI4F4OeFn3V10Kgt0jXpQn"), Utils.toNano(0.8), 3);
        Cell order2 = contract.createOrder(txMsg2);
        byte[] order2Signature2 = contract.signCell(keyPair2, order2);

        // send order-2 signed by 2nd owner (root index 1)
        contract.sendOrder(tonlib, keyPair2, 1, order2, queryId2, List.of(order2Signature2)); // root index 1
        Utils.sleep(15, "processing 2nd query");

        queryState = contract.getQueryState(tonlib, queryId2);
        log.info("get_query_state (query {}): status {}, mask {}", queryId2, queryState.getLeft(), queryState.getRight());

        // send 2nd signature to the 2nd order and thus execute it
        byte[] order2Signature3 = contract.signCell(keyPair3, order2);
        // send order-2 signed by 3rd owner (root index 2)
        contract.sendOrder(tonlib, keyPair3, 2, order2, queryId2, List.of(order2Signature3)); // root index 1
        Utils.sleep(15, "processing 3rd query");

        queryState = contract.getQueryState(tonlib, queryId2);
        log.info("get_query_state (query {}): status {}, mask {}", queryId2, queryState.getLeft(), queryState.getRight());
    }

    private void showMessagesInfo(Map<BigInteger, Cell> messages, int k, String label) {
        if (messages.isEmpty()) {
            log.info("{} result is empty", label);
        }
        for (Map.Entry<BigInteger, Cell> entry : messages.entrySet()) {
            BigInteger query_id = entry.getKey();
            Cell query = entry.getValue();

            CellSlice slice = CellSlice.beginParse(query);
            slice.skipBit();
            BigInteger creatorI = slice.loadUint(8);
            BigInteger cnt = slice.loadUint(8);

            // shows mask (of length k bits) of signed positions, e.g. 101 - first and third signed
            // every query might have different k
            BigInteger cnt_bits = slice.loadUint(k);
            Cell c = slice.sliceToCell();
            log.info("{} query-id {}, creator-i {}, cnt {}, cnt-bits {}, msg {}", label, query_id, creatorI, cnt, cnt_bits, c);
        }
    }
}
