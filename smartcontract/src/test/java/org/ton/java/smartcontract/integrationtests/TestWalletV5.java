package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.types.WalletV5Config;
import org.ton.java.smartcontract.wallet.v5.WalletActions;
import org.ton.java.smartcontract.wallet.v5.WalletV5;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class TestWalletV5 extends CommonTest {
        private static final Address addr1 = Address.of("0QAN9Y278BrYMcbsKKFL4GqvwkIqaIQiSyriFbZDR00zxblI");
        private static final Address addr2 = Address.of("0QCb9KbERyZigZQaeMMF0gjFTJ7cgqeM1wqs4a5ZT3I-Z02R");
        private static final Address addr3 = Address.of("0QBCS04SJWAaSk4AvGD2vg7AHwEU8fH4b4UN0SfNot6rTmBD");


    @Test
    public void testWalletV5Deployment() throws InterruptedException, NoSuchAlgorithmException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        TonHashMapE initExtensions = new TonHashMapE(256);
        WalletV5 contract = WalletV5.builder()
                .isSigAuthAllowed(true)
                .tonlib(tonlib)
                .extensions(initExtensions)
                .keyPair(keyPair)
                .walletId(42)
                .build();

        Address walletAddress = contract.getAddress();

        String nonBounceableAddress = walletAddress.toNonBounceable();
        String bounceableAddress = walletAddress.toBounceable();
        log.info("bounceableAddress: {}", bounceableAddress);
        log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
        log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.2));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        WalletV5Config walletV5Config = WalletV5Config.builder()
                .signatureAllowed(true)
                .seqno(contract.getSeqno())
                .walletId(42)
                .build();

        // deploy wallet-v5
        ExtMessageInfo extMessageInfo = contract.deploy(walletV5Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(60);

        long walletCurrentSeqno = contract.getSeqno();
        log.info("walletV5 balance: {}", Utils.formatNanoValue(contract.getBalance()));
        log.info("seqno: {}", walletCurrentSeqno);
        log.info("walletId: {}", contract.getWalletId());
        log.info("pubKey: {}", Utils.bytesToHex(contract.getPublicKey()));
//        log.info("extensionsList: {}", contract.getRawExtensions());

    }

    @Test
    public void testWalletV5Extensions() throws InterruptedException, NoSuchAlgorithmException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        // initial extensions map
        TonHashMapE initExtensions = new TonHashMapE(256);
//        initExtensions.elements.put(addr1.toBigInteger(), true);
//        initExtensions.elements.put(addr2.toBigInteger(), true);
//        initExtensions.elements.put(addr3.toBigInteger(), false);

        WalletV5 contract = WalletV5.builder()
                .isSigAuthAllowed(true)
                .tonlib(tonlib)
                .extensions(initExtensions)
                .keyPair(keyPair)
                .walletId(42)
                .build();

        Address walletAddress = contract.getAddress();

        String nonBounceableAddress = walletAddress.toNonBounceable();
        String bounceableAddress = walletAddress.toBounceable();
        log.info("bounceableAddress: {}", bounceableAddress);
        log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
        log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.5));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

//        List<Destination> dummyDestinations = createDummyDestinations(3);
//        OutList msgs = createDummyActionSendMessages(255);
        ActionList extensions = createDummyExtendedActions(12, 2);

        WalletV5Config walletV5Config = WalletV5Config.builder()
                .signatureAllowed(false)
                .seqno(contract.getSeqno())
                .walletId(42)
                .body(WalletActions.builder()
//                        .wallet(contract.createBulkTransfer(dummyDestinations))
//                        .extended(extensions)
                        .build())
                .build();

        // deploy wallet-v5
        ExtMessageInfo extMessageInfo = contract.deploy(walletV5Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(60);

        long walletCurrentSeqno = contract.getSeqno();

        walletV5Config = WalletV5Config.builder()
                .signatureAllowed(true)
                .seqno(contract.getSeqno() + 1)
                .walletId(42)
                .body(WalletActions.builder()
//                        .wallet(contract.createBulkTransfer(dummyDestinations))
                        .extended(extensions)
                        .build())
                .build();


        extMessageInfo = contract.send(walletV5Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.getRawExtensions();
        log.info("walletV5 balance: {}", Utils.formatNanoValue(contract.getBalance()));
        log.info("seqno: {}", walletCurrentSeqno);
        log.info("walletId: {}", contract.getWalletId());
        log.info("pubKey: {}", Utils.bytesToHex(contract.getPublicKey()));
//        log.info("extensionsList: {}", extensionsList);
    }

    List<Destination> createDummyDestinations(int count) throws NoSuchAlgorithmException {
        List<Destination> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String dstDummyAddress = "0:" + Utils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));

            result.add(Destination.builder()
                    .bounce(false)
                    .address(dstDummyAddress)
                    .amount(Utils.toNano(0.0001))
                    .comment("Dummy comment wallet-v5. Message id: " + (i + 1))
                    .build());
        }
        return result;
    }

    OutList createDummyActionSendMessages(int numRecipients) throws NoSuchAlgorithmException {
        List<OutAction> actionSendMsgs = new ArrayList<>();
        for (int i = 0; i < numRecipients; i++) {
            Address destinationAddress = Address.of("0:" + Utils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))));
            ActionSendMsg actionSendMsg = ActionSendMsg.builder()
                    .mode(3)
                    .outMsg(MessageRelaxed.builder()
                            .info(InternalMessageInfoRelaxed.builder()
                                    .bounce(true)
                                    .dstAddr(MsgAddressIntStd.builder()
                                            .workchainId(destinationAddress.wc)
                                            .address(destinationAddress.toBigInteger())
                                            .build())
                                    .value(CurrencyCollection.builder()
                                            .coins(Utils.toNano(0.0001))
                                            .build())
                                    .build())
                            .body(CellBuilder.beginCell()
                                    .storeUint(0, 32) // 0 opcode means we have a comment
                                    .storeString("Dummy comment wallet-v5. Message id: " + (i + 1))
                                    .endCell())
                            .build())
                    .build();
            actionSendMsgs.add(actionSendMsg);
        }

        return OutList.builder()
                .actions(actionSendMsgs)
                .build();
    }

    ActionList createDummyExtendedActions(int numExtensions, int actionType) throws NoSuchAlgorithmException {
        List<ExtendedAction> extendedActions = new ArrayList<>();
        for (int i = 0; i < numExtensions; i++) {
            Address destinationAddress = Address.of("0:" + Utils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))));
            ExtendedAction extendedAction = ExtendedAction.builder()
                    .actionType(actionType)
                    .dstAddress(destinationAddress)
                    .build();
            extendedActions.add(extendedAction);
        }

        return ActionList.builder()
                .actions(extendedActions)
                .build();
    }
}
