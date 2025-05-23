package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.highload.HighloadWallet;
import org.ton.ton4j.smartcontract.types.Destination;
import org.ton.ton4j.smartcontract.types.HighloadConfig;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Deprecated
@Slf4j
public class TestHighloadWalletV2 extends CommonTest {

  @Test
  public void testSendTo10() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    BigInteger queryId = BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60L << 32);

    HighloadWallet contract =
        HighloadWallet.builder()
            .tonlib(tonlib)
            .keyPair(keyPair)
            .walletId(42L)
            .queryId(queryId)
            .build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(30);

    HighloadConfig config =
        HighloadConfig.builder()
            .walletId(42)
            .queryId(BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60L << 32))
            .destinations(
                Arrays.asList(
                    Destination.builder()
                        .address("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G")
                        .amount(Utils.toNano(0.2))
                        .comment("test-comment-1")
                        .build(),
                    Destination.builder()
                        .address("EQBrpstctZ5gF-VaaPswcWHe3JQijjNbtJVn5USXlZ-bAgO3")
                        .amount(Utils.toNano(0.1))
                        .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
                        .body(
                            CellBuilder.beginCell()
                                .storeUint(0, 32)
                                .storeString("test-comment-2")
                                .endCell())
                        .build(),
                    Destination.builder()
                        .address("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_")
                        .amount(Utils.toNano(0.3))
                        .build(),
                    Destination.builder()
                        .address("EQAUEeWmzaf2An-MmNi1DRlFAU6ol_qTLP-_LlUnfgF-lA00")
                        .amount(Utils.toNano(0.6))
                        .build(),
                    Destination.builder()
                        .address("EQCwqNA5WhNTTQtl-QDlOlwcBDUS377Q4tRW69V82Q3LXvru")
                        .amount(Utils.toNano(0.2))
                        .build(),
                    Destination.builder()
                        .address("EQAQ93wokze84Loos4arP5aK7AlQFqbg1HDjEogsMCCbZyNo")
                        .amount(Utils.toNano(0.1))
                        .build(),
                    Destination.builder()
                        .address("EQALeq_z73heR4wMQRFKgA_fwkbZgxEf0ya0Kl6UjvpvG-A3")
                        .amount(Utils.toNano(0.15))
                        .build(),
                    Destination.builder()
                        .address("EQCP-ejxzoB6KJ6auhnsPrW1pW6gAZ8uHXnUSHuHGNpY1zJf")
                        .amount(Utils.toNano(0.42))
                        .build(),
                    Destination.builder()
                        .address("EQCkS2OnOOjeLV-LEEUmIPh-_in4pdFr1cScZG1Inft3qUea")
                        .amount(Utils.toNano(0.22))
                        .build(),
                    Destination.builder()
                        .address("EQCZlgy61mcgYNXK0yiFHC9CxjoxxAFkwiUtzTONrk6_Qk6W")
                        .amount(Utils.toNano(0.33))
                        .build()))
            .build();

    // transfer coins to multiple destination as specified in options
    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    log.info("sending to 10 destinations");
    contract.waitForBalanceChange(90);

    balance = contract.getBalance();
    log.info(
        "new wallet {} balance: {}",
        contract.getName(),
        Utils.formatNanoValue(contract.getBalance()));
    assertThat(balance.longValue()).isLessThan(Utils.toNano(3).longValue());
  }

  @Test
  public void testSendTo250() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    BigInteger queryId = BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60L << 32);

    HighloadWallet contract =
        HighloadWallet.builder()
            .tonlib(tonlib)
            .keyPair(keyPair)
            .walletId(42L)
            .queryId(queryId)
            .build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(15));
    Utils.sleep(30, "topping up...");
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(60);

    List<Destination> destinations = generateTargetsWithSameAmountAndSendMode(250);

    HighloadConfig highloadConfig =
        HighloadConfig.builder()
            .walletId(42)
            .queryId(BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60L << 32))
            .destinations(destinations)
            .build();

    extMessageInfo = contract.send(highloadConfig);
    assertThat(extMessageInfo.getError().getCode()).isZero();
  }

  private List<Destination> generateTargetsWithSameAmountAndSendMode(int count) {

    List<Destination> result = new ArrayList<>();

    for (int i = 0; i < count; i++) {

      WalletV3R2 contract = WalletV3R2.builder().build();

      Address dest = contract.getAddress();
      double amount = 0.05;
      log.info(
          "will send {} to {}",
          Utils.formatNanoValue(Utils.toNano(amount)),
          dest.toNonBounceable());

      Destination destination =
          Destination.builder()
              .bounce(false)
              .address(dest.toNonBounceable())
              .amount(Utils.toNano(amount))
              .build();

      result.add(destination);
    }
    return result;
  }
}
