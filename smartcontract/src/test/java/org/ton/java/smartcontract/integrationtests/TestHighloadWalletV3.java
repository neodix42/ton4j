package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
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
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.highloadV3, options);
        HighloadWalletV3 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
        Utils.sleep(30, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        long createdAt = Instant.now().getEpochSecond() - 60 * 5;

        Address destAddress = Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G");

        Cell messageToSendForDeploy = createMessageToSendForDeploy(destAddress, 0, createdAt, keyPair);

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, keyPair.getSecretKey(), messageToSendForDeploy, createdAt);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        Cell cell = createMessageToSend(destAddress, 0.1, createdAt, keyPair);

        HighloadV3Config config = HighloadV3Config
                .builder()
                .body(cell)
                .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
                .createdAt(createdAt)
                .build();

        extMessageInfo = contract.sendTonCoins(tonlib, keyPair.getSecretKey(), config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("sent single message");
    }

    @Test
    public void testBulkPayloadTransfer3DifferentRecipients() throws InterruptedException, NoSuchAlgorithmException {
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
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.highloadV3, options);
        HighloadWalletV3 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        long createdAt = Instant.now().getEpochSecond() - 60 * 5;
        Address destAddress = Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G");

        Cell messageToSendForDeploy = createMessageToSendForDeploy(destAddress, 0, createdAt, keyPair);

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, keyPair.getSecretKey(), messageToSendForDeploy, createdAt);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        int numberOfRecipients = 3;
        BigInteger amountToSendTotal = Utils.toNano(0.01 * numberOfRecipients);

        Cell threeMessages = createNMessages(numberOfRecipients, contract, createdAt);

        Cell extMsgWith3Mgs = createMessageBulkToSend(contract.getAddress(), amountToSendTotal, threeMessages, createdAt, keyPair, contract);

        extMessageInfo = contract.send(tonlib, keyPair.getSecretKey(), extMsgWith3Mgs, createdAt);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("sent {} messages", numberOfRecipients);
    }

    @Test
    public void testBulkPayloadTransfer200DifferentRecipients() throws InterruptedException, NoSuchAlgorithmException {
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
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.highloadV3, options);
        HighloadWalletV3 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        long createdAt = Instant.now().getEpochSecond() - 60 * 5;
        Address destAddress = Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G");

        Cell messageToSendForDeploy = createMessageToSendForDeploy(destAddress, 0, createdAt, keyPair);

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, keyPair.getSecretKey(), messageToSendForDeploy, createdAt);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        int numberOfRecipients = 200;
        BigInteger amountToSendTotal = Utils.toNano(0.01 * numberOfRecipients);

        Cell threeMessages = createNMessages(numberOfRecipients, contract, createdAt);

        Cell extMsgWith3Mgs = createMessageBulkToSend(contract.getAddress(), amountToSendTotal, threeMessages, createdAt, keyPair, contract);

        extMessageInfo = contract.send(tonlib, keyPair.getSecretKey(), extMsgWith3Mgs, createdAt);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("sent {} messages", numberOfRecipients);
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

    Cell createNMessages(int numRecipients, HighloadWalletV3 contract, long createdAt) throws NoSuchAlgorithmException {
        List<OutAction> outActions = new ArrayList<>();
        for (int i = 0; i < numRecipients; i++) {
            Address destinationAddress = Address.of("0:" + Utils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))));
            log.info("dest {} is {}", i, destinationAddress.toString(true));
            OutAction outAction = ActionSendMsg.builder()
                    .mode((byte) 3)
                    .outMsg(MessageRelaxed.builder()
                            .info(InternalMessageInfoRelaxed.builder()
                                    .iHRDisabled(true)
                                    .bounce(true)
                                    .bounced(false)
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

        return HighloadV3InternalMessageBody.builder()
                .queryId(BigInteger.ZERO)
                .actions(OutList.builder()
                        .actions(outActions)
                        .build())
                .build().toCell();
    }

    private Cell createMessageToSendForDeploy(Address destAddress, double amount, long createdAt, TweetNaclFast.Signature.KeyPair keyPair) {

        CommonMsgInfoRelaxed msgToSend = InternalMessageInfoRelaxed.builder() // int_msg_info$0
                .iHRDisabled(true)
                .bounce(true)
                .bounced(false)
                .srcAddr(MsgAddressExtNone.builder().build())
                .dstAddr(MsgAddressIntStd.builder()
                        .workchainId(destAddress.wc)
                        .address(destAddress.toBigInteger())
                        .build())
                .value(CurrencyCollection.builder().coins(Utils.toNano(amount)).build())
                .createdAt(createdAt)
                .build();

        return MessageRelaxed.builder()
                .info(msgToSend)
                .init(null)
                .body(null)
                .build().toCell();
    }

    private Cell createMessageToSend(Address destAddress, double amount, long createdAt, TweetNaclFast.Signature.KeyPair keyPair) {

        CommonMsgInfoRelaxed internalMsg = InternalMessageInfoRelaxed.builder() // int_msg_info$0
                .iHRDisabled(true)
                .bounce(true)
                .bounced(false)
                .srcAddr(MsgAddressExtNone.builder().build())
                .dstAddr(MsgAddressIntStd.builder()
                        .workchainId(destAddress.wc)
                        .address(destAddress.toBigInteger())
                        .build())
                .value(CurrencyCollection.builder().coins(Utils.toNano(amount)).build())
                .createdAt(createdAt)
                .build();

        Cell innerMsg = internalMsg.toCell();
        byte[] signature = new TweetNaclFast.Signature(keyPair.getPublicKey(), keyPair.getSecretKey()).detached(innerMsg.hash());

        Cell externalMessageBody = CellBuilder.beginCell()
                .storeBytes(signature)
                .storeRef(innerMsg)
                .endCell();

        return MessageRelaxed.builder()
                .info(internalMsg)
                .init(null)
                .body(externalMessageBody)
                .build().toCell();
    }

    private Cell createMessageBulkToSend(Address ownAddress, BigInteger totalAmount, Cell bulkMessages, long createdAt, TweetNaclFast.Signature.KeyPair keyPair, HighloadWalletV3 contract) {

        CommonMsgInfoRelaxed internalMsg = InternalMessageInfoRelaxed.builder() // int_msg_info$0
                .iHRDisabled(true)
                .bounce(true)
                .bounced(false)
                .srcAddr(MsgAddressExtNone.builder().build())
                .dstAddr(MsgAddressIntStd.builder()
                        .workchainId(ownAddress.wc)
                        .address(ownAddress.toBigInteger())
                        .build())
                .value(CurrencyCollection.builder().coins(totalAmount).build())
                .createdAt(createdAt)
                .build();

        Cell messageRelaxed = MessageRelaxed.builder()
                .info(internalMsg)
                .init(null)
                .body(bulkMessages)
                .build().toCell();
        return messageRelaxed;
    }
}
