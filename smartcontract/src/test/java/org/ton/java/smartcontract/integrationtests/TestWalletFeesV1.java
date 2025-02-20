package org.ton.java.smartcontract.integrationtests;

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
import org.ton.java.address.Address;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.types.WalletV1R3Config;
import org.ton.java.smartcontract.wallet.v1.WalletV1R3;
import org.ton.java.tlb.Message;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.QueryFees;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletFeesV1 extends CommonTest {

  @Test
  public void testWalletFeesV1() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPairA = Utils.generateSignatureKeyPair();

    WalletV1R3 walletA = WalletV1R3.builder().tonlib(tonlib).keyPair(keyPairA).build();

    String nonBounceableAddrWalletA = walletA.getAddress().toNonBounceable();
    String rawAddrWalletA = walletA.getAddress().toRaw();

    log.info("rawAddressA: {}", rawAddrWalletA);
    log.info("pub-key {}", Utils.bytesToHex(walletA.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(walletA.getKeyPair().getSecretKey()));

    TweetNaclFast.Signature.KeyPair keyPairB = Utils.generateSignatureKeyPair();

    WalletV1R3 walletB = WalletV1R3.builder().tonlib(tonlib).keyPair(keyPairB).build();

    String nonBounceableAddrWalletB = walletB.getAddress().toNonBounceable();
    String rawAddrWalletB = walletB.getAddress().toRaw();

    log.info("rawAddressB: {}", rawAddrWalletB);

    log.info("pub-key {}", Utils.bytesToHex(walletB.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(walletB.getKeyPair().getSecretKey()));

    // top up new walletA using test-faucet-wallet
    BigInteger balance1 =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddrWalletA), Utils.toNano(1));
    log.info("balance walletA: {}", Utils.formatNanoValue(balance1));

    // top up new walletB using test-faucet-wallet
    BigInteger balance2 =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddrWalletB), Utils.toNano(1));
    log.info("balance walletB: {} ", Utils.formatNanoValue(balance2));

    ExtMessageInfo extMessageInfo = walletA.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    walletA.waitForDeployment(30);

    extMessageInfo = walletB.deploy();
    AssertionsForClassTypes.assertThat(extMessageInfo.getError().getCode()).isZero();

    walletB.waitForDeployment(30);

    // transfer 0.1 from walletA to walletB where B receives exact amount i.e. 0.1
    BigInteger balanceAbefore = walletA.getBalance();
    BigInteger balanceBbefore = walletB.getBalance();
    log.info("walletA balance before: {}", Utils.formatNanoValue(balanceAbefore));
    log.info("walletB balance before: {}", Utils.formatNanoValue(balanceBbefore));

    WalletV1R3Config configA =
        WalletV1R3Config.builder()
            .seqno(walletA.getSeqno())
            .destination(walletB.getAddress())
            .mode(3)
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

  @Test
  public void testWithDeployedWalletsV1AB() {

    TweetNaclFast.Box.KeyPair keyPairBoxA =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "e368b936b859c27a632007dac8e3234fd17e25898905583acc0a500275bd1572d185c92173eea4bca516456aeeabf396cc17a4d63fae46eb74b96d1017adf6a7"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureA =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxA.getSecretKey());
    WalletV1R3 walletA = WalletV1R3.builder().keyPair(keyPairSignatureA).tonlib(tonlib).build();
    log.info("rawAddressA {}", walletA.getAddress().toRaw());
    log.info("bounceableA {}", walletA.getAddress().toBounceable());

    TweetNaclFast.Box.KeyPair keyPairBoxB =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "a4f1ccdf31ed77f634013163377d30797cb66d7097a4a87065353485d08445d2bc3d035203c01c744b6f525195a0aa7697b0015ef647bf6391f0b206c77fe75b"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureB =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxB.getSecretKey());
    WalletV1R3 walletB = WalletV1R3.builder().keyPair(keyPairSignatureB).tonlib(tonlib).build();
    log.info("rawAddressB {}", walletB.getAddress().toRaw());
    log.info("bounceableB {}", walletB.getAddress().toBounceable());

    BigInteger balanceAbefore = walletA.getBalance();
    BigInteger balanceBbefore = walletB.getBalance();
    log.info("walletA balance before: {}", Utils.formatNanoValue(balanceAbefore));
    log.info("walletB balance before: {}", Utils.formatNanoValue(balanceBbefore));

    WalletV1R3Config configA =
        WalletV1R3Config.builder()
            .seqno(walletA.getSeqno())
            .destination(walletB.getAddress())
            .mode(3)
            .build();

    Message msg = walletA.prepareExternalMsg(configA);
    QueryFees feesWithCodeData =
        tonlib.estimateFees(walletB.getAddress().toBounceable(), msg.getBody().toBase64());

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
  }

  @Test
  public void testWalletStorageFeeSpeedV1() {

    TweetNaclFast.Box.KeyPair keyPairBoxA =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "e368b936b859c27a632007dac8e3234fd17e25898905583acc0a500275bd1572d185c92173eea4bca516456aeeabf396cc17a4d63fae46eb74b96d1017adf6a7"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureA =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxA.getSecretKey());
    WalletV1R3 walletA = WalletV1R3.builder().keyPair(keyPairSignatureA).tonlib(tonlib).build();
    log.info("rawAddressA {}", walletA.getAddress().toRaw());
    log.info("bounceableA {}", walletA.getAddress().toBounceable());

    TweetNaclFast.Box.KeyPair keyPairBoxB =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "a4f1ccdf31ed77f634013163377d30797cb66d7097a4a87065353485d08445d2bc3d035203c01c744b6f525195a0aa7697b0015ef647bf6391f0b206c77fe75b"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureB =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxB.getSecretKey());
    WalletV1R3 walletB = WalletV1R3.builder().keyPair(keyPairSignatureB).tonlib(tonlib).build();

    WalletV1R3Config configA =
        WalletV1R3Config.builder()
            .seqno(walletA.getSeqno())
            .destination(walletB.getAddress())
            .mode(3)
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
}
