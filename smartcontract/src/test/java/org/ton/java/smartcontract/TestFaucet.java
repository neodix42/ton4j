package org.ton.java.smartcontract;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.types.InitExternalMessage;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v1.SimpleWalletContractR3;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.AccountAddressOnly;
import org.ton.java.tonlib.types.FullAccountState;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestFaucet {

    public static String PUBLIC_KEY = "82ce5343d552c34c24f79856db990865973478ff39df49fa3108f9739fcb0640";
    public static String SECRET_KEY = "52728a26cb50c0ffd036e1d61cadaf62fd11a7e354b05cb97d6519ae45817e2082ce5343d552c34c24f79856db990865973478ff39df49fa3108f9739fcb0640";
    public static String FAUCET_ADDRESS_RAW = "0:1a1875077e4a56146b38dbc1139b29b8bdbbd1371080d68e2c4e4c2a8a02a381";
    public static String NON_BOUNCEABLE = "0QAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgRnw";
    public static String BOUNCEABLE = "kQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgUQ1";

    public static BigInteger topUpContract(Tonlib tonlib, Address destinationAddress, BigInteger amount) throws InterruptedException {
        byte[] secretKey = Utils.hexToBytes(SECRET_KEY);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.simpleR3, options);
        SimpleWalletContractR3 contract = wallet.create();
        log.info("faucet address {}", contract.getAddress().toString(false));

        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), destinationAddress, amount);

        BigInteger newBalance;
        int i = 0;
        do {
            log.info("topping up the wallet...");
            TimeUnit.SECONDS.sleep(5);
            newBalance = new BigInteger(tonlib.getAccountState(destinationAddress).getBalance());
            if (++i > 10) {
                throw new Error("cannot top up the contract " + destinationAddress);
            }
        } while (newBalance.compareTo(BigInteger.ZERO) < 1);

        return newBalance;
    }

    @Test
    public void testFaucetBalance() {
        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .build();
        FullAccountState state = tonlib.getAccountState(AccountAddressOnly.builder().account_address(FAUCET_ADDRESS_RAW).build());
        log.info("TEST FAUCET BALANCE {}", Utils.formatNanoValue(state.getBalance(), 2));
    }

    @Test
    public void createFaucetWallet() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.simpleR3, options);
        SimpleWalletContractR3 contract = wallet.create();
        assertThat(contract.getAddress()).isNotNull();
        log.info("Private key {}", Utils.bytesToHex(keyPair.getSecretKey()));
        log.info("Public key {}", Utils.bytesToHex(keyPair.getPublicKey()));
        log.info("Non-bounceable address (for init): {}", contract.getAddress().toString(true, true, false, true));
        log.info("Bounceable address (for later access): {}", contract.getAddress().toString(true, true, true, true));
        log.info("Unfriendly address: {}", contract.getAddress().toString(false));
    }

    /**
     * Top up the non-bounceable address and then deploy the faucet.
     * Update SECRET_KEY and FAUCET_ADDRESS_RAW variables
     */
    @Test
    public void deployFaucetWallet() {
        byte[] secretKey = Utils.hexToBytes(SECRET_KEY);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.simpleR3, options);
        SimpleWalletContractR3 contract = wallet.create();
        InitExternalMessage msg = contract.createInitExternalMessage(keyPair.getSecretKey());

        log.info("deploying faucet contract to address {}", contract.getAddress().toString(false));
        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .build();
        tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
    }
}