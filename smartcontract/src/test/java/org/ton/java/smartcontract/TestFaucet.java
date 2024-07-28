package org.ton.java.smartcontract;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.types.WalletV1R3Config;
import org.ton.java.smartcontract.wallet.ContractUtils;
import org.ton.java.smartcontract.wallet.v1.WalletV1R3;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.AccountAddressOnly;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.FullAccountState;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;
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
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(SECRET_KEY));

        WalletV1R3 faucet = WalletV1R3.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .build();

        BigInteger faucetBalance = null;
        int i = 0;
        do {
            try {
                if (i++ > 10) {
                    throw new Error("Cannot get faucet balance. Restart.");
                }

                faucetBalance = faucet.getBalance();
                log.info("Faucet address {}, balance {}", faucet.getAddress().toString(true, true, true), Utils.formatNanoValue(faucetBalance));
                if (faucetBalance.compareTo(amount) < 0) {
                    throw new Error("Faucet does not have that much toncoins. faucet balance " + Utils.formatNanoValue(faucetBalance) + ", requested " + Utils.formatNanoValue(amount));
                }
            } catch (Exception e) {
                log.info("Cannot get faucet balance. Restarting...");
                Utils.sleep(5, "Waiting for faucet balance");
            }
        } while (isNull(faucetBalance));

        WalletV1R3Config config = WalletV1R3Config.builder()
                .bounce(false)
                .seqno(faucet.getSeqno())
                .destination(destinationAddress)
                .amount(amount)
                .comment("top-up from ton4j faucet")
                .build();

        ExtMessageInfo extMessageInfo = faucet.send(config);

        if (extMessageInfo.getError().getCode() != 0) {
            throw new Error(extMessageInfo.getError().getMessage());
        }

        ContractUtils.waitForBalanceChange(tonlib, destinationAddress, 60);

        return tonlib.getAccountBalance(destinationAddress);
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
        WalletV1R3 contract = WalletV1R3.builder()
                .build();

        assertThat(contract.getAddress()).isNotNull();
        log.info("Private key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));
        log.info("Public key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
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

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        WalletV1R3 contract = WalletV1R3.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .build();

        log.info("Private key {}", Utils.bytesToHex(keyPair.getSecretKey()));
        log.info("Public key {}", Utils.bytesToHex(keyPair.getPublicKey()));
        String nonBounceableAddress = contract.getAddress().toString(true, true, false, true);
        log.info("Non-bounceable address (for init): {}", nonBounceableAddress);
        log.info("Bounceable address (for later access): {}", contract.getAddress().toString(true, true, true, true));
        log.info("Raw address: {}", contract.getAddress().toString(false));


//        Message msg = contract.createExternalMessage(contract.getAddress(), true, null);
        ExtMessageInfo extMessageInfo = contract.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    @Test
    public void topUpAnyContract() throws InterruptedException {
        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .build();
        BigInteger newBalance = TestFaucet.topUpContract(tonlib, Address.of("Ef_lZ1T4NCb2mwkme9h2rJfESCE0W34ma9lWp7-_uY3zXDvq"), Utils.toNano(1));
        log.info("new balance " + Utils.formatNanoValue(newBalance));
    }
}