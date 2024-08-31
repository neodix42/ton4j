package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.java.address.Address;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV5Config;
import org.ton.java.smartcontract.wallet.v5.WalletActions;
import org.ton.java.smartcontract.wallet.v5.WalletV5;
import org.ton.java.tlb.types.ActionSendMsg;
import org.ton.java.tlb.types.CurrencyCollection;
import org.ton.java.tlb.types.InternalMessageInfoRelaxed;
import org.ton.java.tlb.types.MessageRelaxed;
import org.ton.java.tlb.types.MsgAddressIntStd;
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
public class TestWalletV5Extensions extends CommonTest {
//        TonHashMapE x = new TonHashMapE(10);
//        Address addr1 = Address.of("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3");
//        Address addr2 = Address.of("0:0000000000000000000000000000000000000000000000000000000000000000");
//        Address addr3 = Address.of("0:1111111111111111111111111111111111111111111111111111111111111111");
//        x.elements.put(addr1, true);
//        x.elements.put(addr2, false);
//        x.elements.put(addr3, true);


    @Test
    public void test() throws InterruptedException, NoSuchAlgorithmException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        TonHashMapE initExtensions = new TonHashMapE(10);
        WalletV5 contract = WalletV5.builder()
                .isSignatureAllowed(true)
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

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
//        BigInteger b = contract.getBalance();
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // deploy wallet-v5
        // TODO... breaks on this call
        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(30);

        long walletCurrentSeqno = contract.getSeqno();
        log.info("walletV5 balance: {}", Utils.formatNanoValue(contract.getBalance()));
        log.info("seqno: {}", walletCurrentSeqno);
        log.info("walletId: {}", contract.getWalletId());
        log.info("pubKey: {}", Utils.bytesToHex(contract.getPublicKey()));
        log.info("extensionsList: {}", contract.getRawExtensions());

        // add extension -- start
        WalletV5Config config = WalletV5Config.builder()
                .signatureAllowed(true)
                .seqno(walletCurrentSeqno)
                .walletId(42)
                .extensions(WalletActions.builder()
                        .outSendMessageAction(createDummyActionSendMessages(50))
                        .extendedActions(createDummyExtendedActions(10, WalletActions.Action.ADD_EXTENSION))
                        .build())
                .build();

        Utils.sleep(30);

        extMessageInfo = contract.send(config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        List<String> extensions = contract.getRawExtensions();
        log.info("extensionsList: {}", extensions);

    }

    List<ActionSendMsg> createDummyActionSendMessages(int numRecipients) throws NoSuchAlgorithmException {
        List<ActionSendMsg> actionSendMsgs = new ArrayList<>();
        for (int i = 0; i < numRecipients; i++) {
            Address destinationAddress = Address.of("0:" + Utils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))));
            ActionSendMsg actionSendMsg = ActionSendMsg.builder()
                    .mode((byte) 3)
                    .outMsg(MessageRelaxed.builder()
                            .info(InternalMessageInfoRelaxed.builder()
                                    .bounce(true)
                                    .dstAddr(MsgAddressIntStd.builder()
                                            .workchainId(destinationAddress.wc)
                                            .address(destinationAddress.toBigInteger())
                                            .build())
                                    .value(CurrencyCollection.builder()
                                            .coins(Utils.toNano(0.01))
                                            .build())
                                    .build())
                            .build())
                    .build();
            actionSendMsgs.add(actionSendMsg);
        }

        return actionSendMsgs;
    }

    List<WalletActions.ExtendedAction> createDummyExtendedActions(int numExtensions, WalletActions.Action type) throws NoSuchAlgorithmException {
        List<WalletActions.ExtendedAction> extendedActions = new ArrayList<>();
        for (int i = 0; i < numExtensions; i++) {
            Address destinationAddress = Address.of("0:" + Utils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))));
            WalletActions.ExtendedAction extendedAction = new WalletActions.ExtendedAction(type, destinationAddress);
            extendedActions.add(extendedAction);
        }

        return extendedActions;
    }
}
