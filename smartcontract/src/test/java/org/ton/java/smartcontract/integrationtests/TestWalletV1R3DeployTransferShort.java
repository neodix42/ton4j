package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV1R3Config;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR3;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ton.java.utils.Utils.formatNanoValue;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R3DeployTransferShort extends CommonTest {

    @Test
    public void testNewWalletV1R3() throws InterruptedException {

        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .secretKey(keyPair.getSecretKey())
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.V1R3, options);
        WalletV1ContractR3 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        String status = tonlib.getAccountStatus(Address.of(bounceableAddress));
        log.info("account status {}", status);

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", contract.getName(), formatNanoValue(balance));

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, null);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30);

        // transfer coins from new wallet (back to faucet)
        WalletV1R3Config config = WalletV1R3Config.builder()
                .seqno(contract.getSeqno(tonlib))
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.08))
                .mode(3)
                .comment("testNewWalletV1R2")
                .build();
        contract.sendTonCoins(tonlib, config);

        Utils.sleep(30);

        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
        status = tonlib.getAccountStatus(Address.of(bounceableAddress));
        log.info("new wallet {} with status {} and balance: {}", contract.getName(), status, formatNanoValue(balance));

        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());

        log.info("seqno {}", contract.getSeqno(tonlib));
        log.info("pubkey {}", contract.getPublicKey(tonlib));
    }
}
