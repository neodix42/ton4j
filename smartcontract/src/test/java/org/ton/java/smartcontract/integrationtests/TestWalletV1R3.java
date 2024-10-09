package org.ton.java.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.mnemonic.Mnemonic;
import org.ton.java.mnemonic.Pair;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV1R3Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v1.WalletV1R3;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.QueryFees;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R3 extends CommonTest {

    @Test
    public void testWalletV1R3() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        WalletV1R3 contract = WalletV1R3.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .build();

        Cell deployMessage = CellBuilder.beginCell().storeUint(BigInteger.ZERO, 32).endCell();
        Message msg = MsgUtils.createExternalMessageWithSignedBody(keyPair, contract.getAddress(),
                contract.getStateInit(), deployMessage);
        Address address = msg.getInit().getAddress();

        String nonBounceableAddress = address.toNonBounceable();
        String bounceableAddress = address.toBounceable();

        String my = "Creating new wallet in workchain " + contract.getWc() + "\n";
        my = my + "Loading private key from file new-wallet.pk" + "\n";
        my = my + "StateInit: " + msg.getInit().toCell().print() + "\n";
        my = my + "new wallet address = " + address.toString(false) + "\n";
        my = my + "(Saving address to file new-wallet.addr)" + "\n";
        my = my + "Non-bounceable address (for init): " + nonBounceableAddress + "\n";
        my = my + "Bounceable address (for later access): " + bounceableAddress + "\n";
        my = my + "signing message: " + msg.getBody().print() + "\n";
        my = my + "External message for initialization is " + msg.toCell().print() + "\n";
        my = my + Utils.bytesToHex(msg.toCell().toBoc()).toUpperCase() + "\n";
        my = my + "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
        log.info(my);

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // deploy new wallet
        ExtMessageInfo extMessageInfo = tonlib.sendRawMessage(msg.toCell().toBase64());
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(20);

        WalletV1R3Config config = WalletV1R3Config.builder()
                .seqno(contract.getSeqno())
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.08))
                .comment("testNewWalletV1R3")
                .build();

        // transfer coins from new wallet (back to faucet)
        extMessageInfo = contract.send(config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForBalanceChange(30);

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());

        log.info("seqno {}", contract.getSeqno());
        log.info("pubkey {}", contract.getPublicKey());
    }

    @Test
    public void testWalletV1R3WithMnemonic() throws InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        List<String> mnemonic = Mnemonic.generate(24);
        Pair keyPair = Mnemonic.toKeyPair(mnemonic);

        log.info("pubkey " + Utils.bytesToHex(keyPair.getPublicKey()));
        log.info("seckey " + Utils.bytesToHex(keyPair.getSecretKey()));

        TweetNaclFast.Signature.KeyPair keyPairSig = TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

        log.info("pubkey " + Utils.bytesToHex(keyPairSig.getPublicKey()));
        log.info("seckey " + Utils.bytesToHex(keyPairSig.getSecretKey()));

        WalletV1R3 contract = WalletV1R3.builder()
                .tonlib(tonlib)
                .keyPair(keyPairSig)
                .build();

        Message msg = MsgUtils.createExternalMessageWithSignedBody(keyPairSig, contract.getAddress(),
                contract.getStateInit(),
                CellBuilder.beginCell().storeUint(BigInteger.ZERO, 32).endCell());
        Address address = msg.getInit().getAddress();

        String nonBounceableAddress = address.toBounceable();
        String bounceableAddress = address.toNonBounceable();

        String my = "Creating new wallet in workchain " + contract.getWc() + "\n";
        my = my + "Loading private key from file new-wallet.pk" + "\n";
        my = my + "StateInit: " + msg.getInit().toCell().print() + "\n";
        my = my + "new wallet address = " + address.toString(false) + "\n";
        my = my + "(Saving address to file new-wallet.addr)" + "\n";
        my = my + "Non-bounceable address (for init): " + nonBounceableAddress + "\n";
        my = my + "Bounceable address (for later access): " + bounceableAddress + "\n";
        my = my + "signing message: " + msg.getBody().print() + "\n";
        my = my + "External message for initialization is " + msg.toCell().print() + "\n";
        my = my + Utils.bytesToHex(msg.toCell().toBoc()).toUpperCase() + "\n";
        my = my + "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
        log.info(my);

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // deploy new wallet
        ExtMessageInfo extMessageInfo = tonlib.sendRawMessage(msg.toCell().toBase64());
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(30);

        WalletV1R3Config config = WalletV1R3Config.builder()
                .seqno(contract.getSeqno())
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.08))
                .comment("testNewWalletV1R3")
                .build();

        // transfer coins from new wallet (back to faucet)
        extMessageInfo = contract.send(config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForBalanceChange(30);

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());

        log.info("seqno {}", contract.getSeqno());
        log.info("pubkey {}", contract.getPublicKey());
    }

    @Test
    public void testWalletV1R3EstimateFees() throws NoSuchAlgorithmException, InvalidKeyException {
        List<String> mnemonic = Mnemonic.generate(24);
        Pair keyPair = Mnemonic.toKeyPair(mnemonic);

        log.info("pubkey " + Utils.bytesToHex(keyPair.getPublicKey()));
        log.info("seckey " + Utils.bytesToHex(keyPair.getSecretKey()));

        TweetNaclFast.Signature.KeyPair keyPairSig = TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

        log.info("pubkey " + Utils.bytesToHex(keyPairSig.getPublicKey()));
        log.info("seckey " + Utils.bytesToHex(keyPairSig.getSecretKey()));

        WalletV1R3 contract = WalletV1R3.builder()
                .keyPair(keyPairSig)
                .build();

        Message msg = MsgUtils.createExternalMessageWithSignedBody(keyPairSig, contract.getAddress(),
                contract.getStateInit(), null);

        QueryFees feesWithCodeData = tonlib.estimateFees(
                msg.getInit().getAddress().toString(),
                msg.getBody().toBase64(), // message to cell not the whole external
                msg.getInit().getCode().toBase64(),
                msg.getInit().getData().toBase64(),
                false);

        log.info("fees {}", feesWithCodeData);
        assertThat(feesWithCodeData).isNotNull();

        QueryFees feesBodyOnly = tonlib.estimateFees(
                msg.getInit().getAddress().toString(),
                msg.getBody().toBase64(),
                null,
                null,
                false);
        log.info("fees {}", feesBodyOnly);
        assertThat(feesBodyOnly).isNotNull();
    }
}
