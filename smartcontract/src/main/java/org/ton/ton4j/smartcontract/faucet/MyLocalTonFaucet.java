package org.ton.ton4j.smartcontract.faucet;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

@Slf4j
public class MyLocalTonFaucet {

    static String SECRET_KEY =
            "1bd726fa69d850a5c0032334b16802c7eda48fde7a0e24f28011b22159cc97b7";
    public static String FAUCET_ADDRESS_RAW =
            "0:1da77f0269bbbb76c862ea424b257df63bd1acb0d4eb681b68c9aadfbf553b93";

    public static BigInteger topUpContract(
            Tonlib tonlib, Address destinationAddress, BigInteger amount) throws InterruptedException {

        if (amount.compareTo(Utils.toNano(20)) > 0) {
            throw new Error(
                    "Too many TONs requested from the TestnetFaucet, maximum amount per request is 20.");
        }

        TweetNaclFast.Signature.KeyPair keyPair =
                Utils.generateSignatureKeyPairFromSeed(Utils.hexToSignedBytes(SECRET_KEY));

        WalletV3R2 faucet = WalletV3R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

        BigInteger faucetBalance = null;
        int i = 0;
        do {
            try {
                if (i++ > 10) {
                    throw new Error("Cannot get MyLocalTon faucet balance. Restart.");
                }

                faucetBalance = faucet.getBalance();
                log.info(
                        "MyLocalTon faucet address {}, balance {}",
                        faucet.getAddress().toRaw(),
                        Utils.formatNanoValue(faucetBalance));
                if (faucetBalance.compareTo(amount) < 0) {
                    throw new Error(
                            "MyLocalTon faucet does not have that much toncoins. Faucet balance "
                                    + Utils.formatNanoValue(faucetBalance)
                                    + ", requested "
                                    + Utils.formatNanoValue(amount));
                }
            } catch (Exception e) {
                log.info("Cannot get MyLocalTon faucet balance. Restarting...");
                Utils.sleep(5, "Waiting for MyLocalTon faucet balance");
            }
        } while (isNull(faucetBalance));

        WalletV3Config config =
                WalletV3Config.builder()
                        .bounce(false)
                        .walletId(42)
                        .seqno(faucet.getSeqno())
                        .destination(destinationAddress)
                        .amount(amount)
                        .comment("top-up from ton4j MyLocalTon faucet")
                        .build();

        ExtMessageInfo extMessageInfo = faucet.send(config);

        if (extMessageInfo.getError().getCode() != 0) {
            throw new Error(extMessageInfo.getError().getMessage());
        }

        tonlib.waitForBalanceChange(destinationAddress, 60);

        return tonlib.getAccountBalance(destinationAddress);
    }

    public static BigInteger topUpContract(
            AdnlLiteClient adnlLiteClient, Address destinationAddress, BigInteger amount)
            throws Exception {

        if (amount.compareTo(Utils.toNano(20)) > 0) {
            throw new Error(
                    "Too many TONs requested from the MyLocalTonFaucet, maximum amount per request is 20.");
        }

        TweetNaclFast.Signature.KeyPair keyPair =
                TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(SECRET_KEY));

        WalletV3R2 faucet = WalletV3R2.builder().adnlLiteClient(adnlLiteClient).keyPair(keyPair).build();

        BigInteger faucetBalance = null;
        int i = 0;
        do {
            try {
                if (i++ > 10) {
                    throw new Error("Cannot get testnet faucet balance. Restart.");
                }

                faucetBalance = faucet.getBalance();
                log.info(
                        "MyLocalTon faucet address {}, balance {}",
                        faucet.getAddress().toBounceable(),
                        Utils.formatNanoValue(faucetBalance));
                if (faucetBalance.compareTo(amount) < 0) {
                    throw new Error(
                            "MyLocalTon faucet does not have that much toncoins. Faucet balance "
                                    + Utils.formatNanoValue(faucetBalance)
                                    + ", requested "
                                    + Utils.formatNanoValue(amount));
                }
            } catch (Exception e) {
                log.info("Cannot get MyLocalTon faucet balance. Restarting...");
                Utils.sleep(5, "Waiting for MyLocalTon faucet balance");
            }
        } while (isNull(faucetBalance));

        WalletV3Config config =
                WalletV3Config.builder()
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

        adnlLiteClient.waitForBalanceChange(destinationAddress, 60);
        return adnlLiteClient.getBalance(destinationAddress);
    }
}
