package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.tlb.Message;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.TonResponse;
import org.ton.ton4j.toncenter.model.EstimateFeeResponse;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.QueryFees;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletFeesV3 extends CommonTest {

  /**
   * Trying to send amount of toncoins so that recipient gets the exact amount. There might be a
   * mistake of several nano coins if transaction lasts too long.
   */
  @Test
  public void testWalletFeesV3() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPairA = Utils.generateSignatureKeyPair();

    WalletV3R2 walletA = WalletV3R2.builder().tonlib(tonlib).keyPair(keyPairA).walletId(42).build();

    String nonBounceableAddrWalletA = walletA.getAddress().toNonBounceable();
    String rawAddrWalletA = walletA.getAddress().toRaw();

    log.info("rawAddressA: {}", rawAddrWalletA);
    log.info("pub-key {}", Utils.bytesToHex(walletA.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(walletA.getKeyPair().getSecretKey()));

    TweetNaclFast.Signature.KeyPair keyPairB = Utils.generateSignatureKeyPair();

    WalletV3R2 walletB = WalletV3R2.builder().tonlib(tonlib).keyPair(keyPairB).walletId(98).build();

    String nonBounceableAddrWalletB = walletB.getAddress().toNonBounceable();
    String rawAddrWalletB = walletB.getAddress().toRaw();

    log.info("rawAddressB: {}", rawAddrWalletB);

    log.info("pub-key {}", Utils.bytesToHex(walletB.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(walletB.getKeyPair().getSecretKey()));

    // top up new walletA using test-faucet-wallet
    BigInteger balance1 =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddrWalletA), Utils.toNano(1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        walletA.getWalletId(),
        walletA.getName(),
        Utils.formatNanoValue(balance1));

    // top up new walletB using test-faucet-wallet
    BigInteger balance2 =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddrWalletB), Utils.toNano(1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        walletB.getWalletId(),
        walletB.getName(),
        Utils.formatNanoValue(balance2));

    SendResponse sendResponse = walletA.deploy();
    assertThat(sendResponse.getCode()).isZero();

    walletA.waitForDeployment(30);

    sendResponse = walletB.deploy();
    AssertionsForClassTypes.assertThat(sendResponse.getCode()).isZero();

    walletB.waitForDeployment(30);

    // transfer 0.1 from walletA to walletB where B receives exact amount i.e. 0.1
    BigInteger balanceAbefore = walletA.getBalance();
    BigInteger balanceBbefore = walletB.getBalance();
    log.info("walletA balance before: {}", Utils.formatNanoValue(balanceAbefore));
    log.info("walletB balance before: {}", Utils.formatNanoValue(balanceBbefore));

    WalletV3Config configA =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(walletA.getSeqno())
            .destination(walletB.getAddress())
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .build();

    Message msg = walletA.prepareExternalMsg(configA);

    QueryFees fees =
        tonlib.estimateFees(walletB.getAddress().toBounceable(), msg.getBody().toBase64());

    // adjust amount by including storage fee
    configA.setAmount(
        Utils.toNano(0.1)
            .add(walletA.getGasFees())
            .add(BigInteger.valueOf(fees.getSource_fees().getStorage_fee())));

    log.info("fees on walletB with msg body from A: {}", fees);
    log.info("sending {}", Utils.formatNanoValue(configA.getAmount()));

    walletA.send(configA);

    walletB.waitForBalanceChange(30);

    BigInteger balanceAafter = walletA.getBalance();
    BigInteger balanceBafter = walletB.getBalance();
    log.info("walletA balance after: {}", Utils.formatNanoValue(balanceAafter));
    log.info("walletB balance after: {}", Utils.formatNanoValue(balanceBafter));

    log.info(
        "diff walletA (debited): -{}",
        Utils.formatNanoValue(balanceAbefore.subtract(balanceAafter)));
    log.info(
        "diff walletB (credited): +{}, missing value {}",
        Utils.formatNanoValue(balanceBafter.subtract(balanceBbefore)),
        Utils.formatNanoValue(Utils.toNano(0.1).subtract(balanceBafter.subtract(balanceBbefore))));

    assertThat(Utils.toNano(0.1).subtract(balanceBafter.subtract(balanceBbefore)))
        .isEqualTo(BigInteger.ZERO);
  }

  /**
   * Trying to send amount of toncoins so that recipient gets the exact amount. There might be a
   * mistake of several nano coins if transaction lasts too long.
   */
  @Test
  public void testWithDeployedWalletsAB() {

    TweetNaclFast.Box.KeyPair keyPairBoxA =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "28e1ae64e97c16e93b882bbdd4bde84ed48ab7148f64ab3e78f6b404c924c79a13f3c53b711d1a92dcba43dca7b9fa3b95f6b75faff7f39ce77f1c0eb5cbc730"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureA =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxA.getSecretKey());
    WalletV3R2 walletA =
        WalletV3R2.builder().keyPair(keyPairSignatureA).tonlib(tonlib).walletId(42).build();
    log.info("rawAddressA {}", walletA.getAddress().toRaw());
    log.info("bounceableA {}", walletA.getAddress().toBounceable());

    TweetNaclFast.Box.KeyPair keyPairBoxB =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "dabc0aa5c3883ba7e9f810a051e002d9b88fa54daa21b17508b166a550ff1c74dd9a2d66e2bac9bef666fd01a41ed0d872265502d40757cdd047933021601317"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureB =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxB.getSecretKey());
    WalletV3R2 walletB =
        WalletV3R2.builder().keyPair(keyPairSignatureB).tonlib(tonlib).walletId(98).build();
    log.info("rawAddressB {}", walletB.getAddress().toRaw());
    log.info("bounceableB {}", walletB.getAddress().toBounceable());

    BigInteger balanceAbefore = walletA.getBalance();
    BigInteger balanceBbefore = walletB.getBalance();
    log.info("walletA balance before: {}", Utils.formatNanoValue(balanceAbefore));
    log.info("walletB balance before: {}", Utils.formatNanoValue(balanceBbefore));

    WalletV3Config configA =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(walletA.getSeqno())
            .destination(walletB.getAddress())
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .build();

    Message msg = walletA.prepareExternalMsg(configA);
    QueryFees feesWithCodeData =
        tonlib.estimateFees(
            walletB.getAddress().toBounceable(), msg.getBody().toBase64(), null, null, true);

    // adjust new amount
    configA.setAmount(
        Utils.toNano(0.1)
            .add(walletA.getGasFees())
            .add(BigInteger.valueOf(feesWithCodeData.getSource_fees().getStorage_fee())));

    log.info("fees on walletB with msg body from A: {}", feesWithCodeData);

    walletA.send(configA);

    walletB.waitForBalanceChange(30);

    BigInteger balanceAafter = walletA.getBalance();
    BigInteger balanceBafter = walletB.getBalance();
    log.info("walletA balance after: {}", Utils.formatNanoValue(balanceAafter));
    log.info("walletB balance after: {}", Utils.formatNanoValue(balanceBafter));

    log.info(
        "diff walletA (debited): -{}",
        Utils.formatNanoValue(balanceAbefore.subtract(balanceAafter)));
    log.info(
        "diff walletB (credited): +{}, missing value {}",
        Utils.formatNanoValue(balanceBafter.subtract(balanceBbefore)),
        Utils.formatNanoValue(Utils.toNano(0.1).subtract(balanceBafter.subtract(balanceBbefore))));

    assertThat(Utils.toNano(0.1).subtract(balanceBafter.subtract(balanceBbefore)))
        .isEqualTo(BigInteger.ZERO);
    /*
       send mode 3
       Pay transfer fees separately from the message value
       diff walletA -0.102204003
       diff walletB +0.099959997
       diff walletA -0.102204004
       diff walletB +0.099959996

       diff walletA -0.102204025 (intMsg Forward Fee: 0.000266669 TON + Tx Total Fee 0.001937356 TON
       diff walletB +0.099959975 (0.1 - 0.099959975 = 0.000040025) gasFee 40000 + StorageFee 25

       1ton and deploy - result B 0.998095999
       send 0.1 to B, result 1.098055985 (should be 1.098095999) - difference (0.000040014)
    */
  }

  @Test
  public void testWalletStorageFeeSpeedV3() {

    TweetNaclFast.Box.KeyPair keyPairBoxA =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "28e1ae64e97c16e93b882bbdd4bde84ed48ab7148f64ab3e78f6b404c924c79a13f3c53b711d1a92dcba43dca7b9fa3b95f6b75faff7f39ce77f1c0eb5cbc730"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureA =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxA.getSecretKey());
    WalletV3R2 walletA =
        WalletV3R2.builder().keyPair(keyPairSignatureA).tonlib(tonlib).walletId(42).build();
    log.info("rawAddressA {}", walletA.getAddress().toRaw());
    log.info("bounceableA {}", walletA.getAddress().toBounceable());

    TweetNaclFast.Box.KeyPair keyPairBoxB =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "dabc0aa5c3883ba7e9f810a051e002d9b88fa54daa21b17508b166a550ff1c74dd9a2d66e2bac9bef666fd01a41ed0d872265502d40757cdd047933021601317"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureB =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxB.getSecretKey());
    WalletV3R2 walletB =
        WalletV3R2.builder().keyPair(keyPairSignatureB).tonlib(tonlib).walletId(98).build();

    WalletV3Config configA =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(walletA.getSeqno())
            .destination(walletB.getAddress())
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .build();

    Message msg = walletA.prepareExternalMsg(configA);

    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleAtFixedRate(
        () -> {
          QueryFees f =
              tonlib.estimateFees(
                  walletB.getAddress().toBounceable(), msg.getBody().toBase64(), null, null, true);
          log.info("fees {}", f);
        },
        0,
        15,
        TimeUnit.SECONDS);

    Utils.sleep(600);
  }

  @Test
  public void testWalletFeesV3TonCenter() throws Exception {
    TonCenter tonCenterClient = TonCenter.builder().apiKey(TESTNET_API_KEY).testnet().build();

    TweetNaclFast.Signature.KeyPair keyPairA = Utils.generateSignatureKeyPair();

    WalletV3R2 walletA =
        WalletV3R2.builder()
            .tonCenterClient(tonCenterClient)
            .keyPair(keyPairA)
            .walletId(42)
            .build();

    String nonBounceableAddrWalletA = walletA.getAddress().toNonBounceable();
    String rawAddrWalletA = walletA.getAddress().toRaw();

    log.info("rawAddressA: {}", rawAddrWalletA);
    log.info("pub-key {}", Utils.bytesToHex(walletA.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(walletA.getKeyPair().getSecretKey()));

    TweetNaclFast.Signature.KeyPair keyPairB = Utils.generateSignatureKeyPair();

    WalletV3R2 walletB =
        WalletV3R2.builder()
            .tonCenterClient(tonCenterClient)
            .keyPair(keyPairB)
            .walletId(98)
            .build();

    String nonBounceableAddrWalletB = walletB.getAddress().toNonBounceable();
    String rawAddrWalletB = walletB.getAddress().toRaw();

    log.info("rawAddressB: {}", rawAddrWalletB);

    log.info("pub-key {}", Utils.bytesToHex(walletB.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(walletB.getKeyPair().getSecretKey()));

    // top up new walletA using test-faucet-wallet
    BigInteger balance1 =
        TestnetFaucet.topUpContract(tonCenterClient, Address.of(nonBounceableAddrWalletA), Utils.toNano(1), true);
    log.info(
        "walletId {} new wallet {} balance: {}",
        walletA.getWalletId(),
        walletA.getName(),
        Utils.formatNanoValue(balance1));

    // top up new walletB using test-faucet-wallet
    BigInteger balance2 =
        TestnetFaucet.topUpContract(tonCenterClient, Address.of(nonBounceableAddrWalletB), Utils.toNano(1), true);
    log.info(
        "walletId {} new wallet {} balance: {}",
        walletB.getWalletId(),
        walletB.getName(),
        Utils.formatNanoValue(balance2));

    SendResponse sendResponse = walletA.deploy();
    assertThat(sendResponse.getCode()).isZero();

    walletA.waitForDeployment();

    sendResponse = walletB.deploy();
    assertThat(sendResponse.getCode()).isZero();

    walletB.waitForDeployment();

    // transfer 0.1 from walletA to walletB where B receives exact amount i.e. 0.1
    BigInteger balanceAbefore = walletA.getBalance();
    Utils.sleep(2);
    BigInteger balanceBbefore = walletB.getBalance();
    log.info("walletA balance before: {}", Utils.formatNanoValue(balanceAbefore));
    log.info("walletB balance before: {}", Utils.formatNanoValue(balanceBbefore));
    Utils.sleep(2);
    WalletV3Config configA =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(walletA.getSeqno())
            .destination(walletB.getAddress())
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .build();

    Message msg = walletA.prepareExternalMsg(configA);

    Utils.sleep(2);
    TonResponse<EstimateFeeResponse> fees =
        tonCenterClient.estimateFee(walletB.getAddress().toBounceable(), msg.getBody().toBase64());

    // adjust amount by including storage fee
    configA.setAmount(
        Utils.toNano(0.1)
            .add(walletA.getGasFees())
            .add(BigInteger.valueOf(fees.getResult().getDestinationFees().get(0).getStorageFee()))); // todo

    log.info("fees on walletB with msg body from A: {}", fees);
    log.info("sending {}", Utils.formatNanoValue(configA.getAmount()));

    walletA.send(configA);
    Utils.sleep(2);

    walletB.waitForBalanceChange();

    Utils.sleep(2);
    BigInteger balanceAafter = walletA.getBalance();
    Utils.sleep(2);
    BigInteger balanceBafter = walletB.getBalance();
    log.info("walletA balance after: {}", Utils.formatNanoValue(balanceAafter));
    log.info("walletB balance after: {}", Utils.formatNanoValue(balanceBafter));

    log.info(
        "diff walletA (debited): -{}",
        Utils.formatNanoValue(balanceAbefore.subtract(balanceAafter)));
    log.info(
        "diff walletB (credited): +{}, missing value {}",
        Utils.formatNanoValue(balanceBafter.subtract(balanceBbefore)),
        Utils.formatNanoValue(Utils.toNano(0.1).subtract(balanceBafter.subtract(balanceBbefore))));

    assertThat(Utils.toNano(0.1).subtract(balanceBafter.subtract(balanceBbefore)))
        .isEqualTo(BigInteger.ZERO);
  }
}
