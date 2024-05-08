package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.highload.HighloadWalletV3;
import org.ton.java.smartcontract.types.HighloadQueryId;
import org.ton.java.smartcontract.types.HighloadV3Config;
import org.ton.java.smartcontract.types.HighloadV3InternalMessageBody;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestHighloadWalletV3 extends CommonTest {

    @Test
    public void testSinglePayloadTransfer() throws InterruptedException {
        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .walletId(42L)
                .timeout(60 * 60)
                .build();

        Wallet wallet = new Wallet(WalletVersion.highloadV3, options);
        HighloadWalletV3 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        Utils.sleep(30, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        long createdAt = Instant.now().getEpochSecond() - 60 * 5;
        Address destAddress = Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G");

        HighloadV3Config config = HighloadV3Config.builder()
                .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
                .createdAt(createdAt)
                .build();

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, keyPair.getSecretKey(), config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        Cell messageToSend = contract.createMessageToSend(destAddress, 0.02, createdAt, keyPair);

        config = HighloadV3Config.builder()
                .body(messageToSend)
                .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
                .createdAt(createdAt)
                .build();

        extMessageInfo = contract.sendTonCoins(tonlib, keyPair.getSecretKey(), config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("sent single message");

//        log.info("sending again with the same query-id causes duplicate message response");
//        extMessageInfo = contract.sendTonCoins(tonlib, keyPair.getSecretKey(), config);
//        assertThat(extMessageInfo.getError().getCode()).isZero();
//        log.info("sent single message");
    }

    @Test
    public void testBulkPayloadTransfer_3_DifferentRecipients() throws InterruptedException, NoSuchAlgorithmException {
        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .walletId(42L)
                .timeout(60 * 60)
                .build();

        Wallet wallet = new Wallet(WalletVersion.highloadV3, options);
        HighloadWalletV3 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        long createdAt = Instant.now().getEpochSecond() - 60 * 5;

        HighloadV3Config config = HighloadV3Config.builder()
                .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
                .createdAt(createdAt)
                .build();

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, keyPair.getSecretKey(), config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        int numberOfRecipients = 3;
        BigInteger amountToSendTotal = Utils.toNano(0.01 * numberOfRecipients);

        Cell nMessages = createNMessages(numberOfRecipients, contract, createdAt, null);

        Cell extMsgWith3Mgs = contract.createMessagesToSend(amountToSendTotal, nMessages, createdAt);

        config = HighloadV3Config.builder()
                .body(extMsgWith3Mgs)
                .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
                .createdAt(createdAt)
                .build();

        extMessageInfo = contract.sendTonCoins(tonlib, keyPair.getSecretKey(), config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("sent {} messages", numberOfRecipients);
    }

    @Test
    public void testBulkPayloadTransfer_200_DifferentRecipients() throws InterruptedException, NoSuchAlgorithmException {
        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .walletId(42L)
                .timeout(60 * 60)
                .build();

        Wallet wallet = new Wallet(WalletVersion.highloadV3, options);
        HighloadWalletV3 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(3));
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        long createdAt = Instant.now().getEpochSecond() - 60 * 5;

        HighloadV3Config config = HighloadV3Config.builder()
                .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
                .createdAt(createdAt)
                .build();

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, keyPair.getSecretKey(), config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        int numberOfRecipients = 200;
        BigInteger amountToSendTotal = Utils.toNano(0.01 * numberOfRecipients);

        Cell nMessages = createNMessages(numberOfRecipients, contract, createdAt, null);

        Cell extMsgWith200Mgs = contract.createMessagesToSend(amountToSendTotal, nMessages, createdAt);

        config = HighloadV3Config.builder()
                .body(extMsgWith200Mgs)
                .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
                .createdAt(createdAt)
                .build();

        extMessageInfo = contract.sendTonCoins(tonlib, keyPair.getSecretKey(), config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("sent {} messages", numberOfRecipients);
    }

    @Test
    public void testBulkPayloadTransfer_660_DifferentRecipients() throws InterruptedException, NoSuchAlgorithmException {
        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .walletId(42L)
                .timeout(60 * 60)
                .build();

        Wallet wallet = new Wallet(WalletVersion.highloadV3, options);
        HighloadWalletV3 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(8));
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        long createdAt = Instant.now().getEpochSecond() - 60 * 5;

        HighloadV3Config config = HighloadV3Config.builder()
                .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
                .createdAt(createdAt)
                .build();

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, keyPair.getSecretKey(), config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        Cell nMessages1 = createNMessages(220, contract, createdAt, null);
        Cell nMessages2 = createNMessages(220, contract, createdAt, nMessages1);
        Cell nMessages3 = createNMessages(220, contract, createdAt, nMessages2);
        Cell extMsgWith400Mgs = contract.createMessagesToSend(Utils.toNano(7), nMessages3, createdAt);

        config = HighloadV3Config.builder()
                .body(extMsgWith400Mgs)
                .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
                .createdAt(createdAt)
                .build();

        extMessageInfo = contract.sendTonCoins(tonlib, keyPair.getSecretKey(), config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("sent {} messages", 660);
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

    Cell createNMessages(int numRecipients, HighloadWalletV3 contract, long createdAt, Cell body) throws NoSuchAlgorithmException {
        List<OutAction> outActions = new ArrayList<>();
        for (int i = 0; i < numRecipients; i++) {
            Address destinationAddress = Address.of("0:" + Utils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))));
            log.info("dest {} is {}", i, destinationAddress.toString(true));
            OutAction outAction = ActionSendMsg.builder()
                    .mode((byte) 3)
                    .outMsg(MessageRelaxed.builder()
                            .info(InternalMessageInfoRelaxed.builder()
                                    .bounce(false) // warning, for tests only
                                    .srcAddr(MsgAddressIntStd.builder()
                                            .workchainId(contract.getAddress().wc)
                                            .address(contract.getAddress().toBigInteger())
                                            .build())
                                    .dstAddr(MsgAddressIntStd.builder()
                                            .workchainId(destinationAddress.wc)
                                            .address(destinationAddress.toBigInteger())
                                            .build())
                                    .value(CurrencyCollection.builder()
                                            .coins(Utils.toNano(0.01))
                                            .build())
                                    .createdAt(createdAt)
                                    .build())
                            .build())
                    .build();
            outActions.add(outAction);
        }

        if (nonNull(body)) { // one of those 220 msgs will contain internal message with new 200 recipients
            OutAction outAction = ActionSendMsg.builder()
                    .mode((byte) 3)
                    .outMsg(MessageRelaxed.builder()
                            .info(InternalMessageInfoRelaxed.builder()
                                    .srcAddr(MsgAddressIntStd.builder()
                                            .workchainId(contract.getAddress().wc)
                                            .address(contract.getAddress().toBigInteger())
                                            .build())
                                    .dstAddr(MsgAddressIntStd.builder()
                                            .workchainId(contract.getAddress().wc)
                                            .address(contract.getAddress().toBigInteger())
                                            .build())
                                    .value(CurrencyCollection.builder()
                                            .coins(Utils.toNano(0.01))
                                            .build())
                                    .createdAt(createdAt)
                                    .build())
                            .body(body) // added other 220 msgs
                            .build())
                    .build();
            outActions.add(outAction);
        }

        return HighloadV3InternalMessageBody.builder()
                .queryId(BigInteger.ZERO)
                .actions(OutList.builder()
                        .actions(outActions)
                        .build())
                .build().toCell();
    }
}
