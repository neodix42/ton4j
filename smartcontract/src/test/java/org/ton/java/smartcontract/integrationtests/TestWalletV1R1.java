package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV1R1Config;
import org.ton.java.smartcontract.wallet.v1.WalletV1R1;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RawAccountState;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R1 extends CommonTest {

    @Test
    public void testNewWalletV1R1AutoKeyPair() throws InterruptedException {

        WalletV1R1 contract = WalletV1R1.builder()
                .wc(0)
                .tonlib(tonlib)
                .build();

        Address walletAddress = contract.getAddress();

        log.info("Wallet version {}", contract.getName());
        log.info("Wallet pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
        log.info("Wallet address {}", walletAddress);

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, walletAddress, Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));


        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();
        contract.waitForDeployment(45);

        RawAccountState accountState2 = tonlib.getRawAccountState(walletAddress);
        log.info("raw  accountState {}", accountState2);
        log.info("deployed? {}", contract.isDeployed());

        WalletV1R1Config config = WalletV1R1Config.builder()
                .seqno(1) // V1R1 does not have get_seqno method
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.08))
                .comment("testNewWalletV1R1")
                .build();

        extMessageInfo = contract.send(config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    @Test
    public void testNewWalletV1R1() throws InterruptedException {

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV1R1 contract = WalletV1R1.builder()
                .tonlib(tonlib)
                .wc(0)
                .keyPair(keyPair)
                .build();

        log.info("Wallet version {}", contract.getName());
        log.info("Wallet pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));

        String nonBounceableAddress = contract.getAddress().toNonBounceable();
        String bounceableAddress = contract.getAddress().toBounceable();

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));


        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();
        contract.waitForDeployment(45);

        balance = contract.getBalance();
        log.info("    wallet balance: {}", Utils.formatNanoValue(balance));

        WalletV1R1Config config = WalletV1R1Config.builder()
                .seqno(1)
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.08))
                .comment("testNewWalletV1R1")
                .build();

        // transfer coins from new wallet (back to faucet)
        extMessageInfo = contract.send(config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForBalanceChange(45);

        balance = contract.getBalance();
        log.info("new wallet balance: {}", Utils.formatNanoValue(balance));

        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
    }
}
