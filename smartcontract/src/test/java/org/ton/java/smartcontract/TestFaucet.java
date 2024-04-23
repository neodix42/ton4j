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

    public static String PUBLIC_KEY = "8010a5e0aeb4b4920dd813faec447ba2e963c2338096e74bfe134e523d1e73ac";
    public static String SECRET_KEY = "a4181e01c8ba9b3a634e5d7a57d8b4bf5f18a9f165dd8305a08dac01fa3054b88010a5e0aeb4b4920dd813faec447ba2e963c2338096e74bfe134e523d1e73ac";
    public static String FAUCET_ADDRESS_RAW = "0:96a296d224f285c67bee93c30f8a309157f0daa35dc5b87e410b78630a09cfc7";
    public static String NON_BOUNCEABLE = "0QCWopbSJPKFxnvuk8MPijCRV_Dao13FuH5BC3hjCgnPxy2l";
    public static String BOUNCEABLE = "kQCWopbSJPKFxnvuk8MPijCRV_Dao13FuH5BC3hjCgnPx3Bg";

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
        log.info("Unfriendly address: {}", contract.getAddress().toString(false));
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
        InitExternalMessage msg = contract.createInitExternalMessage(keyPair.getSecretKey());

        log.info("deploying faucet contract to address {}", contract.getAddress().toString(false));
        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .build();
        ExtMessageInfo extMessageInfo = tonlib.sendRawMessage(msg.message.toBase64());
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    @Test
    public void topUpAnyContract() throws InterruptedException {
        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .build();
        TestFaucet.topUpContract(tonlib, Address.of("0QB0gEuvySej-7ZZBAdaBSydBB_oVYUUnp9Ciwm05kJsNKau"), Utils.toNano(5));
    }
}