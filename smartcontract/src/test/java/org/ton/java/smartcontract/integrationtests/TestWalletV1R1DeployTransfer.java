package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV1R1Config;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR1;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R1DeployTransfer extends CommonTest {

    @Test
    public void testNewWalletV1R1GeneratedKeyPair() throws InterruptedException {

        WalletV1ContractR1 contract = WalletV1ContractR1.builder()
                .wc(0)
                .build();

        log.info("Wallet version {}", contract.getName());
        log.info("Wallet pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, contract.getAddress(), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));


        ExtMessageInfo extMessageInfo = contract.deploy(tonlib);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(20, "deploying");

        WalletV1R1Config config = WalletV1R1Config.builder()
                .seqno(1) // V1R1 does not have get_seqno method
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.08))
                .comment("testNewWalletV1R1")
                .build();

        extMessageInfo = contract.send(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    @Test
    public void testNewWalletV1R1() throws InterruptedException {

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV1ContractR1 contract = WalletV1ContractR1.builder()
                .wc(0)
                .keyPair(keyPair)
                .build();

        log.info("Wallet version {}", contract.getName());
        log.info("Wallet pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));


        ExtMessageInfo extMessageInfo = contract.deploy(tonlib);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("deployed");

        Utils.sleep(30, "deploying");

        WalletV1R1Config config = WalletV1R1Config.builder()
                .seqno(1)
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.08))
                .comment("testNewWalletV1R1")
                .build();

        // transfer coins from new wallet (back to faucet)
        extMessageInfo = contract.send(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "sending");

        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
    }
}
