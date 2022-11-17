package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v1.SimpleWalletContractR3;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.AccountState;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R3DeployTransferShort {

    @Test
    public void testNewWalletSimpleR3() throws InterruptedException {

        Tonlib tonlib = Tonlib.builder().testnet(true).build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder().publicKey(keyPair.getPublicKey()).wc(0L).build();

        Wallet wallet = new Wallet(WalletVersion.simpleR3, options);
        SimpleWalletContractR3 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        contract.deploy(tonlib, keyPair.getSecretKey());

        //check if state of the new contract/wallet has changed from un-init to active
        AccountState state;
        int i = 0;
        do {
            Utils.sleep(5);
            state = tonlib.getAccountState(Address.of(bounceableAddress)).getAccount_state();
            if (i++ > 10) {
                throw new Error("time out getting account state");
            }
        } while (StringUtils.isEmpty(state.getCode()));

        log.info("new wallet state: {}", state);

        // transfer coins from new wallet (back to faucet)
        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), Address.of(TestFaucet.BOUNCEABLE), Utils.toNano(0.8));

        Utils.sleep(15);

        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.2).longValue());

        log.info("seqno {}", contract.getSeqno(tonlib));
        log.info("pubkey {}", contract.getPublicKey(tonlib));
    }
}
