package org.ton.ton4j.smartcontract;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.highload.HighloadWalletV3;
import org.ton.ton4j.smartcontract.types.HighloadQueryId;
import org.ton.ton4j.smartcontract.types.HighloadV3Config;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R1;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class GenerateWallet {

  public static WalletV3R1 randomV3R1(Tonlib tonlib, long initialBalanceInToncoins)
      throws InterruptedException {
    log.info("generating WalletV3R1 wallet...");

    WalletV3R1 wallet = WalletV3R1.builder().tonlib(tonlib).wc(0).walletId(42).build();

    Address address = wallet.getAddress();

    String nonBounceableAddress = address.toNonBounceable();
    String bounceableAddress = address.toBounceable();
    String rawAddress = address.toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pubKey: {}", Utils.bytesToHex(wallet.getKeyPair().getPublicKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(
            tonlib, Address.of(nonBounceableAddress), Utils.toNano(initialBalanceInToncoins));
    log.info("new wallet balance {}", Utils.formatNanoValue(balance));

    // deploy new wallet
    SendResponse sendResponse = wallet.deploy();
    assertThat(sendResponse.getCode()).isZero();
    wallet.waitForDeployment(90);

    return wallet;
  }

  public static HighloadWalletV3 randomHighloadV3R1(Tonlib tonlib, long initialBalanceInToncoins)
      throws InterruptedException {

    log.info("generating HighloadWalletV3 wallet...");
    HighloadWalletV3 wallet = HighloadWalletV3.builder().tonlib(tonlib).walletId(42).build();

    String nonBounceableAddress = wallet.getAddress().toNonBounceable();
    String bounceableAddress = wallet.getAddress().toBounceable();
    String rawAddress = wallet.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);

    log.info("pub-key {}", Utils.bytesToHex(wallet.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(wallet.getKeyPair().getSecretKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            tonlib, Address.of(nonBounceableAddress), Utils.toNano(initialBalanceInToncoins));
    Utils.sleep(30, "topping up...");
    log.info("highload wallet {} balance: {}", wallet.getName(), Utils.formatNanoValue(balance));

    HighloadV3Config highloadV3Config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    SendResponse sendResponse = wallet.deploy(highloadV3Config);
    assertThat(sendResponse.getCode()).isZero();

    wallet.waitForDeployment(90);

    // highload v3 wallet deploy - end
    return wallet;
  }

  public static WalletV3R1 randomV3R1(AdnlLiteClient adnlLiteClient, long initialBalanceInToncoins)
      throws Exception {
    log.info("generating WalletV3R1 wallet...");

    WalletV3R1 wallet =
        WalletV3R1.builder().adnlLiteClient(adnlLiteClient).wc(0).walletId(42).build();

    Address address = wallet.getAddress();

    String nonBounceableAddress = address.toNonBounceable();
    String bounceableAddress = address.toBounceable();
    String rawAddress = address.toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pubKey: {}", Utils.bytesToHex(wallet.getKeyPair().getPublicKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(
            adnlLiteClient,
            Address.of(nonBounceableAddress),
            Utils.toNano(initialBalanceInToncoins));
    log.info("new wallet balance {}", Utils.formatNanoValue(balance));

    // deploy new wallet
    SendResponse sendResponse = wallet.deploy();
    assertThat(sendResponse.getCode()).isZero();
    wallet.waitForDeployment(90);

    return wallet;
  }

  public static HighloadWalletV3 randomHighloadV3R1(
      AdnlLiteClient adnlLiteClient, long initialBalanceInToncoins) throws Exception {

    log.info("generating HighloadWalletV3 wallet...");
    HighloadWalletV3 wallet =
        HighloadWalletV3.builder().adnlLiteClient(adnlLiteClient).walletId(42).build();

    String nonBounceableAddress = wallet.getAddress().toNonBounceable();
    String bounceableAddress = wallet.getAddress().toBounceable();
    String rawAddress = wallet.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);

    log.info("pub-key {}", Utils.bytesToHex(wallet.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(wallet.getKeyPair().getSecretKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            adnlLiteClient,
            Address.of(nonBounceableAddress),
            Utils.toNano(initialBalanceInToncoins));
    Utils.sleep(30, "topping up...");
    log.info("highload wallet {} balance: {}", wallet.getName(), Utils.formatNanoValue(balance));

    HighloadV3Config highloadV3Config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    SendResponse sendResponse = wallet.deploy(highloadV3Config);
    assertThat(sendResponse.getCode()).isZero();

    wallet.waitForDeployment(90);

    // highload v3 wallet deploy - end
    return wallet;
  }

  public static WalletV3R1 randomV3R1(TonCenter tonCenter, long initialBalanceInToncoins)
          throws Exception {
    log.info("generating WalletV3R1 wallet...");

    WalletV3R1 wallet = WalletV3R1.builder().tonCenterClient(tonCenter).wc(0).walletId(42).build();

    Address address = wallet.getAddress();

    String nonBounceableAddress = address.toNonBounceable();
    String bounceableAddress = address.toBounceable();
    String rawAddress = address.toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pubKey: {}", Utils.bytesToHex(wallet.getKeyPair().getPublicKey()));

    BigInteger balance =
            TestnetFaucet.topUpContract(
                    tonCenter, Address.of(nonBounceableAddress), Utils.toNano(initialBalanceInToncoins), true);
    log.info("new wallet balance {}", Utils.formatNanoValue(balance));

    // deploy new wallet
    SendResponse sendResponse = wallet.deploy();
    assertThat(sendResponse.getCode()).isZero();
    wallet.waitForDeployment();

    return wallet;
  }
}
