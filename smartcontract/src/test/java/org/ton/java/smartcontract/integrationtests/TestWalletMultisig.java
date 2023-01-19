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

    @Test
    public void testWalletMultisig() throws InterruptedException {

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair ownerKeyPair = Utils.generateSignatureKeyPair();
        TweetNaclFast.Signature.KeyPair keyPair2 = Utils.generateSignatureKeyPair();
        TweetNaclFast.Signature.KeyPair keyPair3 = Utils.generateSignatureKeyPair();

        log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
        log.info("pubKey2 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
        log.info("pubKey3 {}", Utils.bytesToHex(keyPair3.getPublicKey()));

        BigInteger queryId = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 5 * 60L, 32)); //5 minutes // todo

        int k = 2;
        int n = 3;
        Options options = Options.builder()
                .publicKey(ownerKeyPair.getPublicKey())
                .walletId(new Random().nextLong() & 0xffffffffL)
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

        contract.deploy(tonlib, ownerKeyPair.getSecretKey());

        Utils.sleep(30, "deploying"); // with empty ext msg

        log.info("owners publicKeys {}", contract.getPublicKeys(tonlib));
        log.info("owners publicKeysHex {}", contract.getPublicKeysHex(tonlib));
        Pair<Long, Long> n_k = contract.getNandK(tonlib);
        log.info("n {}, k {}", n_k.getLeft(), n_k.getRight());

//         get init-state by input parameters - WORKS
//        List<OwnerInfo> ownersPublicKeys = List.of(
//                OwnerInfo.builder()
//                        .publicKey(ownerKeyPair.getPublicKey())
//                        .flood(0)
//                        .build(),
//                OwnerInfo.builder()
//                        .publicKey(keyPair2.getPublicKey())
//                        .flood(1)
//                        .build()
//        );

//        Cell stateInit = contract.getInitState(tonlib, 1, 3, 2, contract.createOwnersInfosDict(ownersPublicKeys));
//        log.info("state-init {}", stateInit.toHex(false));
//        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
//        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // You may want to create one or several internal messages to send
        Cell msg = contract.createOneInternalMsg(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);

        // Having message(s) to send you can group it to a new order
        Cell order = contract.createOrder(options.getWalletId(), queryId, List.of(msg));

        byte[] orderSignature = contract.signCellHash(ownerKeyPair, order);
        byte[] orderSignature2 = contract.signCellHash(keyPair2, order);
        log.info("orderSignature signed by ownerKeyPair {}", Utils.bytesToHex(orderSignature));

        Cell signedOrder = contract.signOrder(ownerKeyPair, 0, order);

        //create first order
        contract.sendSignedQuery(tonlib, ownerKeyPair, signedOrder, List.of(orderSignature));

        //
        Utils.sleep(15, "processing first query");

        Map<BigInteger, Cell> unsignedMessages = contract.getMessagesUnsigned(tonlib);

        for (Map.Entry<BigInteger, Cell> entry : unsignedMessages.entrySet()) {
            BigInteger query_id = entry.getKey();
            Cell query = entry.getValue();

            CellSlice slice = CellSlice.beginParse(query);
            slice.skipBit();
            BigInteger creatorI = slice.loadUint(8);
            BigInteger cnt = slice.loadUint(8);
            BigInteger cnt_bits = slice.loadUint(k);
            Cell c = slice.sliceToCell(); // todo not this cell but stored in .store_slice(msg)
            log.info("query-id {}, creator-i {}, cnt {}, cnt-bits {}, msg {}", query_id, creatorI, cnt, cnt_bits, c);
            // query-id 9223372036854775807, creator-i 0, cnt 2, cnt-bits 1, msg 0001207_
        }

        Pair<Long, Long> queryState = contract.getQueryState(tonlib, queryId);
//        returns -1 for processed queries, 0 for unprocessed, 1 for unknown (forgotten)
        log.info("get_query_state (query {}): status {} cnt_bits? {}", queryId.toString(10), queryState.getLeft(), queryState.getRight());
        // get_query_state (query 9223372036854775807): status 0 cnt_bits? 1

        Cell query = contract.createQuery(ownerKeyPair, List.of(orderSignature, orderSignature2), order);
        Pair<Long, Long> cnt_mask = contract.checkQuerySignatures(tonlib, query);
        log.info("cnt {}, mask {}", cnt_mask.getLeft(), cnt_mask.getRight());
//        method check_query_signatures, returned an exit code 32
    }
}
