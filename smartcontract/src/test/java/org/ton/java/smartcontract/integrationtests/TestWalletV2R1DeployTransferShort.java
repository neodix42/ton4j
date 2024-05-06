package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV2Config;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v2.WalletV2ContractR1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV2R1DeployTransferShort extends CommonTest {

    @Test
    public void testWalletV2R1() throws InterruptedException {

        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .secretKey(keyPair.getSecretKey())
                .wc(0L).build();

        Wallet wallet = new Wallet(WalletVersion.V2R1, options);
        WalletV2ContractR1 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));


        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, null);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        // transfer coins from new wallet (back to faucet)
        WalletV2Config config = WalletV2Config.builder()
                .seqno(contract.getSeqno(tonlib))
                .destination1(Address.of(TestFaucet.BOUNCEABLE))
                .amount1(Utils.toNano(0.1))
                .build();

        extMessageInfo = contract.sendTonCoins(tonlib, config);

        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "sending to one destination");

        //multi send
        config = WalletV2Config.builder()
                .seqno(contract.getSeqno(tonlib))
                .destination1(Address.of("EQA84DSUMyREa1Frp32wxFATnAVIXnWlYrbd3TFS1NLCbC-B"))
                .destination2(Address.of("EQCJZ3sJnes-o86xOa4LDDug6Lpz23RzyJ84CkTMIuVCCuan"))
                .destination3(Address.of("EQBjS7elE36MmEmE6-jbHQZNEEK0ObqRgaAxXWkx4pDGeefB"))
                .destination4(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"))
                .amount1(Utils.toNano(0.15))
                .amount2(Utils.toNano(0.15))
                .amount3(Utils.toNano(0.15))
                .amount4(Utils.toNano(0.15)).build();

        extMessageInfo = contract.sendTonCoins(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(20, "sending to four destinations");

        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.3).longValue());

        log.info("seqno {}", contract.getSeqno(tonlib));
    }
}
