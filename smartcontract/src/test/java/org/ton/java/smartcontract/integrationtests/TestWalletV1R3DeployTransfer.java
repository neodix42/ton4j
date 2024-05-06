package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.mnemonic.Mnemonic;
import org.ton.java.mnemonic.Pair;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV1R3Config;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR3;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.QueryFees;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R3DeployTransfer extends CommonTest {

    @Test
    public void testNewWalletV1R3() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .secretKey(keyPair.getSecretKey())
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.V1R3, options);
        WalletV1ContractR3 contract = wallet.create();

        Message msg = contract.createExternalMessage(contract.getAddress(),
                true,
                CellBuilder.beginCell().storeUint(BigInteger.ZERO, 32).endCell());
        Address address = msg.getInit().getAddress();

        String nonBounceableAddress = address.toString(true, true, false, true);
        String bounceableAddress = address.toString(true, true, true, true);

        String my = "Creating new wallet in workchain " + options.wc + "\n";
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

        Utils.sleep(30);

        WalletV1R3Config config = WalletV1R3Config.builder()
                .seqno(contract.getSeqno(tonlib))
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.08))
                .mode(3)
                .comment("testNewWalletV1R3")
                .build();

        // transfer coins from new wallet (back to faucet)
        extMessageInfo = contract.sendTonCoins(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30);

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());

        log.info("seqno {}", contract.getSeqno(tonlib));
        log.info("pubkey {}", contract.getPublicKey(tonlib));
    }

    @Test
    public void testNewWalletV1R3WithMnemonic() throws InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        List<String> mnemonic = Mnemonic.generate(24);
        Pair keyPair = Mnemonic.toKeyPair(mnemonic);

        log.info("pubkey " + Utils.bytesToHex(keyPair.getPublicKey()));
        log.info("seckey " + Utils.bytesToHex(keyPair.getSecretKey()));

        TweetNaclFast.Signature.KeyPair keyPairSig = TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

        log.info("pubkey " + Utils.bytesToHex(keyPairSig.getPublicKey()));
        log.info("seckey " + Utils.bytesToHex(keyPairSig.getSecretKey()));

        Options options = Options.builder()
                .publicKey(keyPairSig.getPublicKey())
                .secretKey(keyPairSig.getSecretKey())
                .wc(0L)
                .build();

        WalletV1ContractR3 contract = new Wallet(WalletVersion.V1R3, options).create();

        Message msg = contract.createExternalMessage(contract.getAddress(), true,
                CellBuilder.beginCell().storeUint(BigInteger.ZERO, 32).endCell());
        Address address = msg.getInit().getAddress();

        String nonBounceableAddress = address.toString(true, true, false, true);
        String bounceableAddress = address.toString(true, true, true, true);

        String my = "Creating new wallet in workchain " + options.wc + "\n";
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

        Utils.sleep(25);

        WalletV1R3Config config = WalletV1R3Config.builder()
                .seqno(contract.getSeqno(tonlib))
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.08))
                .mode(3)
                .comment("testNewWalletV1R3")
                .build();

        // transfer coins from new wallet (back to faucet)
        extMessageInfo = contract.sendTonCoins(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(15);

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());

        log.info("seqno {}", contract.getSeqno(tonlib));
        log.info("pubkey {}", contract.getPublicKey(tonlib));
    }

    @Test
    public void testWalletV1EstimateFees() throws NoSuchAlgorithmException, InvalidKeyException {
        List<String> mnemonic = Mnemonic.generate(24);
        Pair keyPair = Mnemonic.toKeyPair(mnemonic);

        log.info("pubkey " + Utils.bytesToHex(keyPair.getPublicKey()));
        log.info("seckey " + Utils.bytesToHex(keyPair.getSecretKey()));

        TweetNaclFast.Signature.KeyPair keyPairSig = TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

        log.info("pubkey " + Utils.bytesToHex(keyPairSig.getPublicKey()));
        log.info("seckey " + Utils.bytesToHex(keyPairSig.getSecretKey()));

        Options options = Options.builder()
                .publicKey(keyPairSig.getPublicKey())
                .secretKey(keyPairSig.getSecretKey())
                .wc(0L)
                .build();

        WalletV1ContractR3 contract = new Wallet(WalletVersion.V1R3, options).create();


        Message msg = contract.createExternalMessage(contract.getAddress(), true, null);

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
