package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.lockup.LockupWalletV1;
import org.ton.java.smartcontract.types.LockupConfig;
import org.ton.java.smartcontract.types.LockupWalletV1Config;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestLockupWallet extends CommonTest {

    @Test
    public void testNewWalletLockup() throws InterruptedException {

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        LockupWalletV1 contract = LockupWalletV1.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .walletId(42)
                .lockupConfig(LockupConfig.builder()
                        .configPublicKey(Utils.bytesToHex(keyPair.getPublicKey()))
                        // important to specify totalRestrictedValue! otherwise wallet will send to prohibited addresses
                        // can be more than total balance wallet
                        .totalRestrictedValue(Utils.toNano(5_000_000))
                        .allowedDestinations(Arrays.asList(
                                TestFaucet.BOUNCEABLE,
                                "kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"))
                        .build())
                .build();

        Address address = contract.getAddress();

        String nonBounceableAddress = address.toNonBounceable();
        String bounceableAddress = address.toBounceable();
        String rawAddress = address.toRaw();

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", rawAddress);
        log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
        log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        log.info("new {} wallet balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(60);

        log.info("seqno {}", contract.getSeqno());
        log.info("sub-wallet id {}", contract.getWalletId());
        log.info("public key {}", contract.getPublicKey());

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance()));
        log.info("restricted balance {}", Utils.formatNanoValue(contract.getNominalRestrictedBalance()));
        log.info("time-locked balance {}", Utils.formatNanoValue(contract.getNominalLockedBalance()));

        // below returns -1 - means true
        log.info("destination 1 allowed {}", contract.check_destination(TestFaucet.BOUNCEABLE));
        log.info("destination 2 allowed {}", contract.check_destination("kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"));
        log.info("destination 3 allowed {}", contract.check_destination("EQDZno6LOWYJRHPpRv-MM3qrhFPk6OHOxVOg1HvEEAtJxK3y"));

        // try to transfer coins from new lockup wallet to allowed address (back to faucet)
        log.info("sending toncoins to allowed address...");
        LockupWalletV1Config config = LockupWalletV1Config.builder()
                .seqno(contract.getSeqno())
                .walletId(42)
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(4))
                .comment("send-to-allowed-1")
                .build();

        extMessageInfo = contract.send(config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(50);

        balance = contract.getBalance();
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(4).longValue());

        log.info("sending toncoins to prohibited address 1st time ...");
        config = LockupWalletV1Config.builder()
                .seqno(contract.getSeqno())
                .walletId(42)
                .destination(Address.of("EQDZno6LOWYJRHPpRv-MM3qrhFPk6OHOxVOg1HvEEAtJxK3y"))
                .amount(Utils.toNano(1.5))
                .comment("send-to-prohibited-1")
                .build();
        extMessageInfo = contract.send(config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(50);

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance()));
        log.info("restricted balance {}", Utils.formatNanoValue(contract.getNominalRestrictedBalance()));
        log.info("time-locked balance {}", Utils.formatNanoValue(contract.getNominalLockedBalance()));

        balance = contract.getBalance();
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));

        log.info("sending toncoins to prohibited address 2nd time ...");
        config = LockupWalletV1Config.builder()
                .seqno(contract.getSeqno())
                .walletId(42)
                .destination(Address.of("0f_N_wfrFUwuWVkwpqmkRRYIJRzByJRobEwRCJTeQ8lq06n9"))
                .amount(Utils.toNano(1.6))
                .comment("send-to-prohibited-2")
                .build();
        extMessageInfo = contract.send(config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(50);

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance()));
        log.info("restricted balance {}", Utils.formatNanoValue(contract.getNominalRestrictedBalance()));
        log.info("time-locked balance {}", Utils.formatNanoValue(contract.getNominalLockedBalance()));

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));


        assertThat(balance.longValue()).isGreaterThan(Utils.toNano(0.9).longValue());

        log.info("sending toncoins to allowed address...");
        config = LockupWalletV1Config.builder()
                .seqno(contract.getSeqno())
                .walletId(42)
                .destination(Address.of("kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"))
                .amount(Utils.toNano(0.5))
                .build();
        extMessageInfo = contract.send(config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(50);

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance()));
        log.info("restricted balance {}", Utils.formatNanoValue(contract.getNominalRestrictedBalance()));
        log.info("time-locked balance {}", Utils.formatNanoValue(contract.getNominalLockedBalance()));

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.7).longValue());
    }

    @Test
    public void testDeployWalletLockup() throws IOException, InterruptedException {

        Address elector = Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
        Address myWallet = Address.of("kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj");

        TweetNaclFast.Box.KeyPair boxKeyPair = Utils.generateKeyPair();
        TweetNaclFast.Signature.KeyPair sigKeyPair = Utils.generateSignatureKeyPairFromSeed(boxKeyPair.getSecretKey());

        log.info("restricted-validator-wallet-001.pk {}", Utils.bytesToHex(boxKeyPair.getSecretKey()));

        // echo 'hex-prv-key' | xxd -r -p  > /usr/local/bin/mytoncore/wallets/restricted-validator-wallet-001.pk

        LockupWalletV1 contract = LockupWalletV1.builder()
                .tonlib(tonlib)
                .keyPair(sigKeyPair)
                .walletId(42)
                .lockupConfig(LockupConfig.builder()
                        .configPublicKey(Utils.bytesToHex(sigKeyPair.getPublicKey())) // same as owner
                        .totalRestrictedValue(Utils.toNano(5_000_000))
                        .allowedDestinations(Arrays.asList(
                                elector.toString(),
                                myWallet.toString())
                        ).build())
                .build();

        Address address = contract.getAddress();

        String nonBounceableAddress = address.toNonBounceable();
        String bounceableAddress = address.toBounceable();

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);


//        Tonlib tonlib = Tonlib.builder().testnet(true).build();
        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        log.info("new {} wallet balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(60);

        log.info("seqno {}", contract.getSeqno());
        address.saveToFile("restricted-validator-wallet-001.addr");
    }
}
