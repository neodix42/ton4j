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
import org.ton.java.smartcontract.types.WalletV4R2Config;
import org.ton.java.smartcontract.wallet.v4.WalletV4R2;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.QueryFees;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletFeesV4 extends CommonTest {

  /**
   * Trying to send amount of toncoins so that recipient gets the exact amount. There might be a
   * mistake of several nano coins if transaction lasts too long.
   */
  @Test
  public void testWalletFeesV4() throws InterruptedException {

    Tonlib tonlib = Tonlib.builder().testnet(true).ignoreCache(false).build();

    TweetNaclFast.Signature.KeyPair keyPairA = Utils.generateSignatureKeyPair();

    WalletV4R2 walletA = WalletV4R2.builder().tonlib(tonlib).keyPair(keyPairA).walletId(42).build();

    String nonBounceableAddrWalletA = walletA.getAddress().toNonBounceable();
    String rawAddrWalletA = walletA.getAddress().toRaw();

    log.info("rawAddressA: {}", rawAddrWalletA);
    log.info("pub-key {}", Utils.bytesToHex(walletA.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(walletA.getKeyPair().getSecretKey()));

    TweetNaclFast.Signature.KeyPair keyPairB = Utils.generateSignatureKeyPair();

    WalletV4R2 walletB = WalletV4R2.builder().tonlib(tonlib).keyPair(keyPairB).walletId(98).build();

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

    WalletV4R2Config configA =
        WalletV4R2Config.builder()
            .walletId(42)
            .seqno(walletA.getSeqno())
            .destination(walletB.getAddress())
            //                .amount(Utils.toNano(0.1 + 0.000040000))
            .mode(3)
            .build();

    Message msg = walletA.prepareExternalMsg(configA);

    QueryFees fees =
        tonlib.estimateFees(
            walletB.getAddress().toBounceable(), msg.getBody().toBase64(), null, null, true);

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
  public void testWithDeployedWalletsV4AB() {

    Tonlib tonlib = Tonlib.builder().testnet(true).ignoreCache(false).build();

    TweetNaclFast.Box.KeyPair keyPairBoxA =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "3d3015dcc3c0f3d51710bfe4894de954334bb6f44f75061109d9e7dcaf7b5a85c2ca482a19f06f63c9975fd7e679d89014ab2b4b645fc591c287a2818dabb42f"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureA =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxA.getSecretKey());
    WalletV4R2 walletA =
        WalletV4R2.builder().keyPair(keyPairSignatureA).tonlib(tonlib).walletId(42).build();
    log.info("rawAddressA {}", walletA.getAddress().toRaw());
    log.info("bounceableA {}", walletA.getAddress().toBounceable());

    TweetNaclFast.Box.KeyPair keyPairBoxB =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "6763fd614cd1658c4f0c0b08dd82d49060f329387c4d38f1281db7ccb91f6eb3219d2acaa90c2091813785426feb5234af9027637db05b55e65ff3e54a94ad0b"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureB =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxB.getSecretKey());
    WalletV4R2 walletB =
        WalletV4R2.builder().keyPair(keyPairSignatureB).tonlib(tonlib).walletId(98).build();
    log.info("rawAddressB {}", walletB.getAddress().toRaw());
    log.info("bounceableB {}", walletB.getAddress().toBounceable());

    BigInteger balanceAbefore = walletA.getBalance();
    BigInteger balanceBbefore = walletB.getBalance();
    log.info("walletA balance before: {}", Utils.formatNanoValue(balanceAbefore));
    log.info("walletB balance before: {}", Utils.formatNanoValue(balanceBbefore));

    WalletV4R2Config configA =
        WalletV4R2Config.builder()
            .walletId(42)
            .seqno(walletA.getSeqno())
            .destination(walletB.getAddress())
            .amount(Utils.toNano(0.1))
            .mode(3)
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
  }

  @Test
  public void testWalletStorageFeeSpeedV4() {

    Tonlib tonlib = Tonlib.builder().testnet(true).ignoreCache(false).build();

    TweetNaclFast.Box.KeyPair keyPairBoxA =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "3d3015dcc3c0f3d51710bfe4894de954334bb6f44f75061109d9e7dcaf7b5a85c2ca482a19f06f63c9975fd7e679d89014ab2b4b645fc591c287a2818dabb42f"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureA =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxA.getSecretKey());
    WalletV4R2 walletA =
        WalletV4R2.builder().keyPair(keyPairSignatureA).tonlib(tonlib).walletId(42).build();
    log.info("rawAddressA {}", walletA.getAddress().toRaw());
    log.info("bounceableA {}", walletA.getAddress().toBounceable());

    TweetNaclFast.Box.KeyPair keyPairBoxB =
        Utils.generateKeyPairFromSecretKey(
            Utils.hexToSignedBytes(
                "6763fd614cd1658c4f0c0b08dd82d49060f329387c4d38f1281db7ccb91f6eb3219d2acaa90c2091813785426feb5234af9027637db05b55e65ff3e54a94ad0b"));
    TweetNaclFast.Signature.KeyPair keyPairSignatureB =
        Utils.generateSignatureKeyPairFromSeed(keyPairBoxB.getSecretKey());
    WalletV4R2 walletB =
        WalletV4R2.builder().keyPair(keyPairSignatureB).tonlib(tonlib).walletId(98).build();

    WalletV4R2Config configA =
        WalletV4R2Config.builder()
            .walletId(42)
            .seqno(walletA.getSeqno())
            .destination(walletB.getAddress())
            .mode(3)
            .build();

    Message msg = walletA.prepareExternalMsg(configA);

    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleAtFixedRate(
        () -> {
          QueryFees f =
              tonlib.estimateFees(walletB.getAddress().toBounceable(), msg.getBody().toBase64());
          log.info("fees {}", f);
        },
        0,
        15,
        TimeUnit.SECONDS);

    Utils.sleep(600);
  }
}
