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
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR3;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.AccountAddressOnly;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.FullAccountState;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestFaucet {

    public static String PUBLIC_KEY = "c02ece00eceb299066597ccc7a8ac0b2d08f0ad425f28c0ea92e74e2064f41f0";
    public static String SECRET_KEY = "46aab91daaaa375d40588384fdf7e36c62d0c0f38c46adfea7f9c904c5973d97c02ece00eceb299066597ccc7a8ac0b2d08f0ad425f28c0ea92e74e2064f41f0";
    public static String FAUCET_ADDRESS_RAW = "0:b52a16ba3735501df19997550e7ed4c41754ee501ded8a841088ce4278b66de4";
    public static String NON_BOUNCEABLE = "0QC1Kha6NzVQHfGZl1UOftTEF1TuUB3tioQQiM5CeLZt5FIA";
    public static String BOUNCEABLE = "kQC1Kha6NzVQHfGZl1UOftTEF1TuUB3tioQQiM5CeLZt5A_F";

    public static BigInteger topUpContract(Tonlib tonlib, Address destinationAddress, BigInteger amount) throws InterruptedException {
        byte[] secretKey = Utils.hexToSignedBytes(SECRET_KEY);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.V1R3, options);
        WalletV1ContractR3 faucet = wallet.create();

        BigInteger faucetBalance = null;
        int i = 0;
        do {
            try {
                if (i++ > 10) {
                    throw new Error("Cannot get faucet balance. Restart.");
                }

                faucetBalance = new BigInteger(tonlib.getAccountState(faucet.getAddress()).getBalance());
                log.info("Faucet address {}, balance {}", faucet.getAddress().toString(true, true, true), Utils.formatNanoValue(faucetBalance));
                if (faucetBalance.compareTo(amount) < 0) {
                    throw new Error("Faucet does not have that much toncoins. faucet balance" + Utils.formatNanoValue(faucetBalance) + ", requested " + Utils.formatNanoValue(amount));
                }
            } catch (Exception e) {
                log.info("Cannot get faucet balance. Restarting...");
                Utils.sleep(5, "Waiting for faucet balance");
            }
        } while (isNull(faucetBalance));

        faucet.sendTonCoins(tonlib, keyPair.getSecretKey(), destinationAddress, amount, "top-up from ton4j");

        BigInteger newBalance = BigInteger.ZERO;
        i = 0;
        do {
            log.info("topping up wallet {}", destinationAddress.toString(true, true, true));
            TimeUnit.SECONDS.sleep(5);
            if (nonNull(tonlib.getAccountState(destinationAddress).getBalance())) {
                newBalance = new BigInteger(tonlib.getAccountState(destinationAddress).getBalance());
            }
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
                .ignoreCache(false)
                .build();
        FullAccountState state = tonlib.getAccountState(AccountAddressOnly.builder().account_address(FAUCET_ADDRESS_RAW).build());
        log.info("account {}", state);
        log.info("TEST FAUCET BALANCE {}", Utils.formatNanoValue(state.getBalance(), 2));
    }

    @Test
    public void createFaucetWallet() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.V1R3, options);
        WalletV1ContractR3 contract = wallet.create();
        assertThat(contract.getAddress()).isNotNull();
        log.info("Private key {}", Utils.bytesToHex(keyPair.getSecretKey()));
        log.info("Public key {}", Utils.bytesToHex(keyPair.getPublicKey()));
        log.info("Non-bounceable address (for init): {}", contract.getAddress().toString(true, true, false, true));
        log.info("Bounceable address (for later access): {}", contract.getAddress().toString(true, true, true, true));
        log.info("Raw address: {}", contract.getAddress().toString(false));
    }

    /**
     * Top up the non-bounceable address and then deploy the faucet.
     * Update SECRET_KEY and FAUCET_ADDRESS_RAW variables
     */
    @Test
    public void deployFaucetWallet() {
        byte[] secretKey = Utils.hexToSignedBytes(SECRET_KEY);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.V1R3, options);
        WalletV1ContractR3 contract = wallet.create();
        log.info("Private key {}", Utils.bytesToHex(keyPair.getSecretKey()));
        log.info("Public key {}", Utils.bytesToHex(keyPair.getPublicKey()));
        log.info("Non-bounceable address (for init): {}", contract.getAddress().toString(true, true, false, true));
        log.info("Bounceable address (for later access): {}", contract.getAddress().toString(true, true, true, true));
        log.info("Raw address: {}", contract.getAddress().toString(false));
        InitExternalMessage msg = contract.createInitExternalMessage(keyPair.getSecretKey());

        log.info("deploying faucet contract to address {}", contract.getAddress().toString(false));
        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .build();
        ExtMessageInfo extMessageInfo = tonlib.sendRawMessage(msg.message.toBase64());
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    @Test
    public void topUpAnyContract() throws InterruptedException {
        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .build();
        TestFaucet.topUpContract(tonlib, Address.of("0QB0gEuvySej-7ZZBAdaBSydBB_oVYUUnp9Ciwm05kJsNKau"), Utils.toNano(0.1));
    }
}