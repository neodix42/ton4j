package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR3;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ton.java.utils.Utils.formatNanoValue;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R3DeployTransferShort extends CommonTest {

    @Test
    public void testNewWalletV1R3() throws InterruptedException {

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder().publicKey(keyPair.getPublicKey()).wc(0L).build();

        Wallet wallet = new Wallet(WalletVersion.V1R3, options);
        WalletV1ContractR3 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        String status = tonlib.getAccountStatus(Address.of(bounceableAddress));
        log.info("account status {}", status);

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
        log.info("new wallet {} balance: {}", contract.getName(), formatNanoValue(balance));

        contract.deploy(tonlib, keyPair.getSecretKey());

        Utils.sleep(30);

        // transfer coins from new wallet (back to faucet)
        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), Address.of(TestFaucet.BOUNCEABLE), Utils.toNano(0.8));

        Utils.sleep(30);

        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
        status = tonlib.getAccountStatus(Address.of(bounceableAddress));
        log.info("new wallet {} with status {} and balance: {}", contract.getName(), status, formatNanoValue(balance));

        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.2).longValue());

        log.info("seqno {}", contract.getSeqno(tonlib));
        log.info("pubkey {}", contract.getPublicKey(tonlib));
    }
}
