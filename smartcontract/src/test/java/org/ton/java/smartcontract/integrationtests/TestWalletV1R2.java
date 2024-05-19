package org.ton.java.smartcontract.integrationtests;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV1R2Config;
import org.ton.java.smartcontract.wallet.v1.WalletV1R2;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R2 extends CommonTest {

    @Test
    public void testNewWalletV1R2() throws InterruptedException {

        WalletV1R2 contract = WalletV1R2.builder()
                .tonlib(tonlib)
                .initialSeqno(2)
                .build();

        String nonBounceableAddress = contract.getAddress().toNonBounceable();
        String bounceableAddress = contract.getAddress().toBounceable();

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        log.info("wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));


        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForDeployment(45);

        log.info("wallet seqno: {}", contract.getSeqno());

        WalletV1R2Config config = WalletV1R2Config.builder()
                .seqno(contract.getSeqno())
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.08))
                .comment("testNewWalletV1R2")
                .build();

        // transfer coins from new wallet (back to faucet)
        extMessageInfo = contract.sendTonCoins(config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        contract.waitForBalanceChange(45);

        balance = contract.getBalance();
        log.info("wallet {} new balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        log.info("wallet seqno: {}", contract.getSeqno());
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
    }
}
