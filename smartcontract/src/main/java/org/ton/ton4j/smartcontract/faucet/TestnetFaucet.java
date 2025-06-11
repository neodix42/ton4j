package org.ton.ton4j.smartcontract.faucet;

import static java.util.Objects.isNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.types.WalletV1R3Config;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R3;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class TestnetFaucet {

  public static String PUBLIC_KEY =
      "c02ece00eceb299066597ccc7a8ac0b2d08f0ad425f28c0ea92e74e2064f41f0";
  static String SECRET_KEY =
      "46aab91daaaa375d40588384fdf7e36c62d0c0f38c46adfea7f9c904c5973d97c02ece00eceb299066597ccc7a8ac0b2d08f0ad425f28c0ea92e74e2064f41f0";
  public static String FAUCET_ADDRESS_RAW =
      "0:b52a16ba3735501df19997550e7ed4c41754ee501ded8a841088ce4278b66de4";
  public static String NON_BOUNCEABLE = "0QC1Kha6NzVQHfGZl1UOftTEF1TuUB3tioQQiM5CeLZt5FIA";
  public static String BOUNCEABLE = "kQC1Kha6NzVQHfGZl1UOftTEF1TuUB3tioQQiM5CeLZt5A_F";

  public static BigInteger topUpContract(
      Tonlib tonlib, Address destinationAddress, BigInteger amount) throws InterruptedException {

    if (amount.compareTo(Utils.toNano(20)) > 0) {
      throw new Error(
          "Too many TONs requested from the TestnetFaucet, maximum amount per request is 20.");
    }

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(SECRET_KEY));

    WalletV1R3 faucet = WalletV1R3.builder().tonlib(tonlib).keyPair(keyPair).build();

    BigInteger faucetBalance = null;
    int i = 0;
    do {
      try {
        if (i++ > 10) {
          throw new Error("Cannot get testnet faucet balance. Restart.");
        }

        faucetBalance = faucet.getBalance();
        log.info(
            "Testnet faucet address {}, balance {}",
            faucet.getAddress().toBounceable(),
            Utils.formatNanoValue(faucetBalance));
        if (faucetBalance.compareTo(amount) < 0) {
          throw new Error(
              "Testnet faucet does not have that much toncoins. Faucet balance "
                  + Utils.formatNanoValue(faucetBalance)
                  + ", requested "
                  + Utils.formatNanoValue(amount));
        }
      } catch (Exception e) {
        log.info("Cannot get testnet faucet balance. Restarting...");
        Utils.sleep(5, "Waiting for testnet faucet balance");
      }
    } while (isNull(faucetBalance));

    WalletV1R3Config config =
        WalletV1R3Config.builder()
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

    faucet.waitForBalanceChange(60);

    return faucet.getBalance();
  }

  public static BigInteger topUpContract(
      AdnlLiteClient adnlLiteClient, Address destinationAddress, BigInteger amount)
      throws InterruptedException {

    if (amount.compareTo(Utils.toNano(20)) > 0) {
      throw new Error(
          "Too many TONs requested from the TestnetFaucet, maximum amount per request is 20.");
    }

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(SECRET_KEY));

    WalletV1R3 faucet =
        WalletV1R3.builder().adnlLiteClient(adnlLiteClient).keyPair(keyPair).build();

    BigInteger faucetBalance = null;
    int i = 0;
    do {
      try {
        if (i++ > 10) {
          throw new Error("Cannot get testnet faucet balance. Restart.");
        }

        faucetBalance = faucet.getBalance();
        log.info(
            "Testnet faucet address {}, balance {}",
            faucet.getAddress().toBounceable(),
            Utils.formatNanoValue(faucetBalance));
        if (faucetBalance.compareTo(amount) < 0) {
          throw new Error(
              "Testnet faucet does not have that much toncoins. Faucet balance "
                  + Utils.formatNanoValue(faucetBalance)
                  + ", requested "
                  + Utils.formatNanoValue(amount));
        }
      } catch (Exception e) {
        log.info("Cannot get testnet faucet balance. Restarting...");
        Utils.sleep(5, "Waiting for testnet faucet balance");
      }
    } while (isNull(faucetBalance));

    WalletV1R3Config config =
        WalletV1R3Config.builder()
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

    faucet.waitForBalanceChange(60);

    return faucet.getBalance();
  }
}
