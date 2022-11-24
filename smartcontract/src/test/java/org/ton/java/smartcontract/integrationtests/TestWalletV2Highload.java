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
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR2;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV2Highload {

    @Test
    public void testWalletV2HighloadSendTo10() throws InterruptedException {

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .walletId(42L)
                .highloadQueryId(BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 5 * 60L, 32))
                        .add(new BigInteger(String.valueOf(Instant.now().getEpochSecond()))))
                .wc(0L)

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

        Utils.sleep(30, "deploying");

        HighloadConfig highloadConfig = HighloadConfig.builder()
                .queryId(BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 5 * 60L, 32)))
                .destinations(List.of(
                        Destination.builder()
                                .address(Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G"))
                                .amount(Utils.toNano(0.2))
                                .mode((byte) 3)
                                .build(),
                        Destination.builder()
                                .address(Address.of("EQBrpstctZ5gF-VaaPswcWHe3JQijjNbtJVn5USXlZ-bAgO3"))
                                .amount(Utils.toNano(0.1))
                                .mode((byte) 3)
                                .build(),
                        Destination.builder()
                                .address(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"))
                                .amount(Utils.toNano(0.3))
                                .build(),
                        Destination.builder()
                                .address(Address.of("EQAUEeWmzaf2An-MmNi1DRlFAU6ol_qTLP-_LlUnfgF-lA00"))
                                .amount(Utils.toNano(0.6))
                                .build(),
                        Destination.builder()
                                .address(Address.of("EQCwqNA5WhNTTQtl-QDlOlwcBDUS377Q4tRW69V82Q3LXvru"))
                                .amount(Utils.toNano(0.2))
                                .build(),
                        Destination.builder()
                                .address(Address.of("EQAQ93wokze84Loos4arP5aK7AlQFqbg1HDjEogsMCCbZyNo"))
                                .amount(Utils.toNano(0.1))
                                .build(),
                        Destination.builder()
                                .address(Address.of("EQALeq_z73heR4wMQRFKgA_fwkbZgxEf0ya0Kl6UjvpvG-A3"))
                                .amount(Utils.toNano(0.15))
                                .build(),
                        Destination.builder()
                                .address(Address.of("EQCP-ejxzoB6KJ6auhnsPrW1pW6gAZ8uHXnUSHuHGNpY1zJf"))
                                .amount(Utils.toNano(0.42))
                                .build()
                        ,
                        Destination.builder()
                                .address(Address.of("EQCkS2OnOOjeLV-LEEUmIPh-_in4pdFr1cScZG1Inft3qUea"))
                                .amount(Utils.toNano(0.22))
                                .build()
                        ,
                        Destination.builder()
                                .address(Address.of("EQCZlgy61mcgYNXK0yiFHC9CxjoxxAFkwiUtzTONrk6_Qk6W"))
                                .amount(Utils.toNano(0.33))
                                .build()
                ))
                .build();

        // transfer coins to multiple destination as specified in options
        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), highloadConfig);

        Utils.sleep(60, "sending to multiple destinations");

        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(3).longValue());
    }

    @Test
    public void testWalletV2HighloadSendTo84() throws InterruptedException {

        Tonlib tonlib = Tonlib.builder()
                .verbosityLevel(VerbosityLevel.DEBUG)
                .testnet(true)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .walletId(42L)
                .highloadQueryId(BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 5 * 60L, 32))
                        .add(new BigInteger(String.valueOf(Instant.now().getEpochSecond()))))
                .wc(0L)

                .build();

        Wallet wallet = new Wallet(WalletVersion.highload, options);
        HighloadWallet contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(7));
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        contract.deploy(tonlib, keyPair.getSecretKey());

        Utils.sleep(30, "deploying");

        // Sends to up to 84 destinations
        List<Destination> destinations = generateTargetsWithSameAmountAndSendMode(84, keyPair.getPublicKey());

        HighloadConfig highloadConfig = HighloadConfig.builder()
                .queryId(BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 5 * 60L, 32)))
                .destinations(destinations)
                .build();

        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), highloadConfig);
    }

    private List<Destination> generateTargetsWithSameAmountAndSendMode(int count, byte[] publicKey) {

        List<Destination> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {

            Options options = Options.builder()
                    .walletId(new Random().nextLong() & 0xffffffffL)
                    .publicKey(publicKey)
                    .build();

            Wallet wallet = new Wallet(WalletVersion.v3R2, options);
            WalletV3ContractR2 contract = wallet.create();
            Address dest = contract.getAddress();
            double amount = 0.05;
            log.info("send {} to {}", Utils.formatNanoValue(Utils.toNano(amount)), dest.toString(true, true, true));

            Destination destination = Destination.builder()
                    .address(dest)
                    .amount(Utils.toNano(amount))
                    .build();

            result.add(destination);
        }
        return result;
    }
}
