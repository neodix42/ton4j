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
import java.time.Instant;
import java.util.Date;
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

        Date date = new Date();
        long timestamp = (long) Math.floor(date.getTime() / 1e3);
        long queryId0 = timestamp + 5 * 60L;
        long queryId1 = timestamp + 6 * 60L;
        long queryId2 = timestamp + 7 * 60L;

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .walletId(42L)
                .wc(0L)
                .highloadConfig(HighloadConfig.builder()
//                        .queryId(List.of(queryId1, queryId2))
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
        Utils.sleep(5, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        contract.deploy(tonlib, keyPair.getSecretKey());

        Utils.sleep(15, "deploying");

        // transfer coins to multiple destination as specified in options
        contract.sendTonCoins(tonlib, keyPair.getSecretKey());

        Utils.sleep(25, "sending to multiple destinations");

        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.3).longValue());
    }

    @Test
    public void a() {
        long timestamp = Instant.now().getEpochSecond();
        long queryId = (long) Math.pow(timestamp + 2 * 60L, 32); // << 32 ; 5 minutes
        log.info("{} {}", timestamp, queryId);

        timestamp = Instant.now().getEpochSecond();
        BigInteger i = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 5 * 60L, 32)).add(new BigInteger(String.valueOf(Instant.now().getEpochSecond())));
        log.info("i {}", i);
        queryId = (long) Math.pow(timestamp + 2 * 60L, 32) + 1; // << 32 ; 5 minutes
        log.info("{} {}", timestamp, queryId);

    }
}
