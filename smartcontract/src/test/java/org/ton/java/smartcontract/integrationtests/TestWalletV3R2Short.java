package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV3R2Short extends CommonTest {

    @Test
    public void testWalletV3R2() throws InterruptedException {

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        WalletV3R2 contract1 = WalletV3R2.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .walletId(42)
                .build();

        String nonBounceableAddress1 = contract1.getAddress().toNonBounceable();
        String bounceableAddress1 = contract1.getAddress().toBounceable();

        log.info("non-bounceable address 1: {}", nonBounceableAddress1);
        log.info("    bounceable address 1: {}", bounceableAddress1);
        String status = tonlib.getAccountStatus(Address.of(bounceableAddress1));
        log.info("account status {}", status);

        WalletV3R2 contract2 = WalletV3R2.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .walletId(98)
                .build();

        String nonBounceableAddress2 = contract2.getAddress().toNonBounceable();
        String bounceableAddress2 = contract2.getAddress().toBounceable();

        log.info("non-bounceable address 2: {}", nonBounceableAddress2);
        log.info("    bounceable address 2: {}", bounceableAddress2);

        // top up new wallet using test-faucet-wallet
        BigInteger balance1 = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress1), Utils.toNano(1));
        log.info("walletId {} new wallet {} balance: {}", contract1.getWalletId(), contract1.getName(), Utils.formatNanoValue(balance1));

        BigInteger balance2 = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress2), Utils.toNano(1));
        log.info("walletId {} new wallet {} balance: {}", contract2.getWalletId(), contract2.getName(), Utils.formatNanoValue(balance2));


        Utils.sleep(15, "balance");

        ExtMessageInfo extMessageInfo = contract1.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract1.waitForDeployment(30);

        extMessageInfo = contract2.deploy();
        AssertionsForClassTypes.assertThat(extMessageInfo.getError().getCode()).isZero();

        contract2.waitForDeployment(30);

        WalletV3Config config = WalletV3Config.builder()
                .subWalletId(42)
                .seqno(contract1.getSeqno())
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.8))
                .comment("testWalletV3R2-42")
                .build();

        // transfer coins from new wallet (back to faucet)
        extMessageInfo = contract1.sendTonCoins(config);
        AssertionsForClassTypes.assertThat(extMessageInfo.getError().getCode()).isZero();

        contract1.waitForBalanceChange(90);

        config = WalletV3Config.builder()
                .subWalletId(98)
                .seqno(contract2.getSeqno())
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.8))
                .comment("testWalletV3R2-98")
                .build();

        extMessageInfo = contract2.sendTonCoins(config);
        AssertionsForClassTypes.assertThat(extMessageInfo.getError().getCode()).isZero();

        contract2.waitForBalanceChange(90);

        balance1 = contract1.getBalance();
        log.info("walletId {} new wallet {} balance: {}", contract1.getWalletId(), contract1.getName(), Utils.formatNanoValue(balance1));

        balance2 = contract2.getBalance();
        log.info("walletId {} new wallet {} balance: {}", contract2.getWalletId(), contract2.getName(), Utils.formatNanoValue(balance2));

        log.info("1 seqno {}", contract1.getSeqno());
        log.info("1 pubkey {}", contract1.getPublicKey());

        log.info("2 seqno {}", contract2.getSeqno());
        log.info("2 pubkey {}", contract2.getPublicKey());

        assertThat(contract1.getPublicKey()).isEqualTo(contract2.getPublicKey());
    }
}
