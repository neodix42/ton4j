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
import org.ton.java.smartcontract.wallet.v2.WalletV2ContractR1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.AccountState;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV2R1DeployTransferShort {

    @Test
    public void testWalletV2R1() throws InterruptedException {

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder().publicKey(keyPair.getPublicKey()).wc(0L).build();

        Wallet wallet = new Wallet(WalletVersion.V2R1, options);
        WalletV2ContractR1 contract = wallet.create();

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
        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), Address.of(TestFaucet.BOUNCEABLE), Utils.toNano(0.1));
        Utils.sleep(20, "sending to one destination");

        //multi send
        contract.sendTonCoins(tonlib, keyPair.getSecretKey(),
                Address.of("EQA84DSUMyREa1Frp32wxFATnAVIXnWlYrbd3TFS1NLCbC-B"),
                Address.of("EQCJZ3sJnes-o86xOa4LDDug6Lpz23RzyJ84CkTMIuVCCuan"),
                Address.of("EQBjS7elE36MmEmE6-jbHQZNEEK0ObqRgaAxXWkx4pDGeefB"),
                Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"),
                Utils.toNano(0.15));

        Utils.sleep(20, "sending to four destinations");

        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.3).longValue());

        log.info("seqno {}", contract.getSeqno(tonlib));
    }
}
