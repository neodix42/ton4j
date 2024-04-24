package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.highload.HighloadWalletV3;
import org.ton.java.smartcontract.types.HighloadQueryId;
import org.ton.java.smartcontract.types.HighloadV3Config;
import org.ton.java.smartcontract.types.HighloadV3InternalMessageBody;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.types.ExtMessageInfo;
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

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, keyPair.getSecretKey());
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(60, "deploying");

        HighloadV3Config config = HighloadV3Config
                .builder()
                .amount(Utils.toNano(0.01))
                .body(null)
                .createdAt(Instant.now().getEpochSecond() - 10)
                .destination(Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G"))
                .mode((byte) 3)
                .queryId(0)
                .build();

        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), config);
    }

    @Test
    public void testBulkPayloadTransfer3DifferentRecipients() throws InterruptedException {
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

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, keyPair.getSecretKey());
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(60, "deploying");

        Address destinationAddress1 = Address.of("EQAI26OclRjgcBsTNtpMxjxJPsjICEXML83p1PEobtB7QlWc");
        Address destinationAddress2 = Address.of("EQAT9oH1KUBOvPs2tIg8hWa-_dnEOZxkLD7PACj0RyP4WfF0");
        Address destinationAddress3 = Address.of("EQBjX1ny_NMJWKonBnszL708F0T2hls99vqEbYr_oD8cHlfv");

        BigInteger amountToSendTotal = Utils.toNano(0.01 + 0.02 + 0.03);
        long createdAt = Instant.now().getEpochSecond() - 60 * 5;

        OutAction outAction1 = ActionSendMsg.builder()
                .mode((byte) 3)
                .outMsg(MessageRelaxed.builder()
                        .info(InternalMessage.builder()
                                .iHRDisabled(true)
                                .bounce(true)
                                .bounced(false)
                                .srcAddr(MsgAddressIntStd.builder()
                                        .workchainId(contract.getAddress().wc)
                                        .address(contract.getAddress().toBigInteger())
                                        .build())
                                .dstAddr(MsgAddressIntStd.builder()
                                        .workchainId(destinationAddress1.wc)
                                        .address(destinationAddress1.toBigInteger())
                                        .build())
                                .value(CurrencyCollection.builder()
                                        .coins(Utils.toNano(0.01))
                                        .build())
                                .createdAt(createdAt)
                                .build())
                        .build())
                .build();

        OutAction outAction2 = ActionSendMsg.builder()
                .mode((byte) 3)
                .outMsg(MessageRelaxed.builder()
                        .info(InternalMessage.builder()
                                .iHRDisabled(true)
                                .bounce(true)
                                .bounced(false)
                                .srcAddr(MsgAddressIntStd.builder()
                                        .workchainId(contract.getAddress().wc)
                                        .address(contract.getAddress().toBigInteger())
                                        .build())
                                .dstAddr(MsgAddressIntStd.builder()
                                        .workchainId(destinationAddress2.wc)
                                        .address(destinationAddress2.toBigInteger())
                                        .build())
                                .value(CurrencyCollection.builder()
                                        .coins(Utils.toNano(0.02))
                                        .build())
                                .createdAt(createdAt)
                                .build())
                        .build())
                .build();

        OutAction outAction3 = ActionSendMsg.builder()
                .mode((byte) 3)
                .outMsg(MessageRelaxed.builder()
                        .info(InternalMessage.builder()
                                .iHRDisabled(true)
                                .bounce(true)
                                .bounced(false)
                                .srcAddr(MsgAddressIntStd.builder()
                                        .workchainId(contract.getAddress().wc)
                                        .address(contract.getAddress().toBigInteger())
                                        .build())
                                .dstAddr(MsgAddressIntStd.builder()
                                        .workchainId(destinationAddress3.wc)
                                        .address(destinationAddress3.toBigInteger())
                                        .build())
                                .value(CurrencyCollection.builder()
                                        .coins(Utils.toNano(0.03))
                                        .build())
                                .createdAt(createdAt)
                                .build())
                        .build())
                .build();

        HighloadV3InternalMessageBody highloadV3InternalMessageBody =
                HighloadV3InternalMessageBody.builder()
                        .queryId(BigInteger.ZERO)
                        .actions(OutList.builder()
                                .actions(List.of(outAction1, outAction2, outAction3))
                                .build())
                        .build();

        HighloadV3Config config = HighloadV3Config
                .builder()
                .amount(amountToSendTotal)
                .body(highloadV3InternalMessageBody.toCell())
                .createdAt(createdAt)
                .destination(contract.getAddress()) // to this contract
                .mode((byte) 3)
                .queryId(0)
                .build();

        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), config);
    }

    @Test
    public void testBulkPayloadTransfer1000Recipients() throws InterruptedException, NoSuchAlgorithmException {
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
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(11));
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, keyPair.getSecretKey());
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");


        long createdAt = Instant.now().getEpochSecond() - 60 * 5;
        List<OutAction> outActions = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Address destinationAddress = Address.of("0:" + Utils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))));
            log.info("dest {} is {}", i, destinationAddress.toString(true));
            OutAction outAction = ActionSendMsg.builder()
                    .mode((byte) 3)
                    .outMsg(MessageRelaxed.builder()
                            .info(InternalMessage.builder()
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

        BigInteger amountToSendTotal = Utils.toNano(0.01 * 1000);


        HighloadV3InternalMessageBody highloadV3InternalMessageBody =
                HighloadV3InternalMessageBody.builder()
                        .queryId(BigInteger.ZERO)
                        .actions(OutList.builder()
                                .actions(outActions)
                                .build())
                        .build();

        log.info("highloadV3InternalMessageBody: {}", highloadV3InternalMessageBody);

        HighloadV3Config config = HighloadV3Config
                .builder()
                .amount(amountToSendTotal)
                .body(highloadV3InternalMessageBody.toCell())
                .createdAt(createdAt)
                .destination(contract.getAddress()) // to this contract
                .mode((byte) 3)
                .queryId(0)
                .build();

        log.info("\nbulk sending... ");

        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), config);
    }

    @Test
    public void testHighloadV3MessageBody() {
        Address destinationAddress = Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G");
        int numberOfTargetRecipients = 3;
        BigInteger amountToSendPerMsg = Utils.toNano(0.01);
        BigInteger amountToSendTotal = amountToSendPerMsg.multiply(BigInteger.valueOf(numberOfTargetRecipients));
        long createdAt = Instant.now().getEpochSecond() - 10;

        OutAction outAction = ActionSendMsg.builder()
                .mode((byte) 3)
                .outMsg(MessageRelaxed.builder()
                        .info(InternalMessage.builder()
                                .iHRDisabled(true)
                                .bounce(true)
                                .bounced(false)
                                .srcAddr(MsgAddressIntStd.builder()
                                        .workchainId((byte) -1)
                                        .address(BigInteger.TWO)
                                        .build())
                                .dstAddr(MsgAddressIntStd.builder()
                                        .workchainId(destinationAddress.wc)
                                        .address(destinationAddress.toBigInteger())
                                        .build())
                                .value(CurrencyCollection.builder()
                                        .coins(amountToSendPerMsg)
                                        .build())
                                .createdAt(createdAt)
                                .build())
                        .build())
                .build();

        HighloadV3InternalMessageBody highloadV3InternalMessageBody =
                HighloadV3InternalMessageBody.builder()
                        .queryId(BigInteger.ZERO)
                        .actions(OutList.builder()
                                .actions(List.of(outAction, outAction, outAction))
                                .build())
                        .build();

        Cell cell1 = highloadV3InternalMessageBody.toCell();
        log.info("cell {}", cell1.print());
        HighloadV3InternalMessageBody deserialized = HighloadV3InternalMessageBody.deserialize(CellSlice.beginParse(cell1));
        log.info("deserialized {}", deserialized);
        Cell cell2 = highloadV3InternalMessageBody.toCell();
        log.info("cell {}", cell2.print());
        assertThat(cell1.print()).isEqualTo(cell2.print());
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
}
