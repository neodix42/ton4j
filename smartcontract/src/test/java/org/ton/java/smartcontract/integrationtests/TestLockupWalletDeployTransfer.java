package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.lockup.LockupWalletV1;
import org.ton.java.smartcontract.types.InitExternalMessage;
import org.ton.java.smartcontract.types.LockupConfig;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestLockupWalletDeployTransfer {

    @Test
    public void testNewWalletLockup() throws InterruptedException {
        byte[] secondPublicKey = Utils.hexToBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
//        byte[] secondSecretKey = Utils.hexToBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .lockupConfig(LockupConfig.builder()
                        .configPublicKey(Utils.bytesToHex(secondPublicKey))
                        // important to specify totalRestrictedValue! otherwise wallet will send to prohibited addresses
                        // can be more than total balance wallet
                        .totalRestrictedValue(Utils.toNano(3))
                        .allowedDestinations(List.of(
                                TestFaucet.BOUNCEABLE,
                                "kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"))
                        .build())
                .build();

        Wallet wallet = new Wallet(WalletVersion.lockup, options);
        LockupWalletV1 contract = wallet.create();

        InitExternalMessage msg = contract.createInitExternalMessage(keyPair.getSecretKey());
        Address address = msg.address;

        String nonBounceableAddress = address.toString(true, true, false);
        String bounceableAddress = address.toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        assertThat(msg.code).isNotNull();

        // top up new wallet using test-faucet-wallet
        Tonlib tonlib = Tonlib.builder().testnet(true).build();

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(3));
        log.info("new {} wallet balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        Utils.sleep(5);

        contract.deploy(tonlib, keyPair.getSecretKey());

        Utils.sleep(30, "deploying");

        log.info("seqno {}", contract.getSeqno(tonlib));
        log.info("sub-wallet id {}", contract.getWalletId(tonlib));
        log.info("public key {}", contract.getPublicKey(tonlib));

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance(tonlib)));
        log.info("restricted balance {}", contract.getNominalRestrictedBalance(tonlib));
        log.info("time-locked balance {}", contract.getNominalLockedBalance(tonlib));
        // below returns -1 - means true
        log.info("destination allowed {}", contract.check_destination(tonlib, TestFaucet.BOUNCEABLE));
        log.info("destination allowed {}", contract.check_destination(tonlib, "kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"));
        log.info("destination allowed {}", contract.check_destination(tonlib, "EQDZno6LOWYJRHPpRv-MM3qrhFPk6OHOxVOg1HvEEAtJxK3y"));

        // try to transfer coins from new lockup wallet to allowed address (back to faucet)
        log.info("sending toncoins to allowed address...");
        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), Address.of(TestFaucet.BOUNCEABLE), Utils.toNano(1));
        Utils.sleep(70);

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(2).longValue());

        log.info("sending toncoins to prohibited address...");
        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), Address.of("EQDZno6LOWYJRHPpRv-MM3qrhFPk6OHOxVOg1HvEEAtJxK3y"), Utils.toNano(1.5));
        Utils.sleep(70);

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isGreaterThan(Utils.toNano(1.9).longValue());

        log.info("sending toncoins to allowed address...");
        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), Address.of("kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"), Utils.toNano(1.3));
        Utils.sleep(70);

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.7).longValue());
    }
}
