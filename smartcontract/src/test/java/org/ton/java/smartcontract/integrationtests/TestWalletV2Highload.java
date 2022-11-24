package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.highload.HighloadWallet;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.types.HighloadConfig;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV2Highload {

    @Test
    public void testWalletV2Highload() throws InterruptedException {

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .walletId(42L)
                .wc(0L)
                .highloadConfig(HighloadConfig.builder()
                        .destinations(List.of(
                                Destination.builder()
                                        .address(Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G"))
                                        .amount(Utils.toNano(1))
                                        .mode((byte) 3)
                                        .build(),
                                Destination.builder()
                                        .address(Address.of("EQBrpstctZ5gF-VaaPswcWHe3JQijjNbtJVn5USXlZ-bAgO3"))
                                        .amount(Utils.toNano(1))
                                        .mode((byte) 3)
                                        .build()
//                                Destination.builder()
//                                        .address(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"))
//                                        .amount(Utils.toNano(0.08))
//                                        .build(),
//                                Destination.builder()
//                                        .address(Address.of("EQAUEeWmzaf2An-MmNi1DRlFAU6ol_qTLP-_LlUnfgF-lA00"))
//                                        .amount(Utils.toNano(0.6))
//                                        .build(),
//                                Destination.builder()
//                                        .address(Address.of("EQCwqNA5WhNTTQtl-QDlOlwcBDUS377Q4tRW69V82Q3LXvru"))
//                                        .amount(Utils.toNano(0.2))
//                                        .build(),
//                                Destination.builder()
//                                        .address(Address.of("EQAQ93wokze84Loos4arP5aK7AlQFqbg1HDjEogsMCCbZyNo"))
//                                        .amount(Utils.toNano(0.1))
//                                        .build(),
//                                Destination.builder()
//                                        .address(Address.of("EQALeq_z73heR4wMQRFKgA_fwkbZgxEf0ya0Kl6UjvpvG-A3"))
//                                        .amount(Utils.toNano(0.15))
//                                        .build(),
//                                Destination.builder()
//                                        .address(Address.of("EQCP-ejxzoB6KJ6auhnsPrW1pW6gAZ8uHXnUSHuHGNpY1zJf"))
//                                        .amount(Utils.toNano(0.42))
//                                        .build()

                                //EQCkS2OnOOjeLV-LEEUmIPh-_in4pdFr1cScZG1Inft3qUea
                                //EQCZlgy61mcgYNXK0yiFHC9CxjoxxAFkwiUtzTONrk6_Qk6W
                                //EQAt_kmFWgz5BJbPExcmUVdfz_go05xGSGbyfPGNjtsNacHu
                                //EQDO8yw0Dbs1loUBMtoBtT-V-YMSlZdQ_FwZWmyeoN9KpanF
                                //EQAsEVrV7tQtgoqzpxqYDdX-KLmPxQkfqRbj8RYtAcWdSWJh
                                //EQBFSkhPfFmwwherHCWnrqryc9zTu0HkV23Ya5EzD-FZjEz-
                                //EQC1ZVhIHkxQ-IKGK3htrFV90CRBbEfayJC4bzmPoeernBau
                                //EQALy9XBBfJZ4rZzSkZM65LYglzJ5ORbGbvN16NjbcigXliy
                                //EQAGashg_KuPjp1CbmU1ic-YTTKTMsbzpDc9PoqoeSRr-Fdz
                                //EQDJxUadaFJoa_0K_twQHawyJkkyv9vlui_ZsUQRA1mw0HVO
                        ))
                        .build())
                .build();

        Wallet wallet = new Wallet(WalletVersion.highload, options);
        HighloadWallet contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        contract.deploy(tonlib, keyPair.getSecretKey());

        Utils.sleep(25, "deploying");

        // transfer coins to multiple destination as specified in options
        contract.sendTonCoins(tonlib, keyPair.getSecretKey());

        Utils.sleep(30, "sending to multiple destinations");

        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(3).longValue());
    }
}
