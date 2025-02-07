package org.ton.java.smartcontract.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.highload.HighloadWalletV3;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.types.HighloadQueryId;
import org.ton.java.smartcontract.types.HighloadV3Config;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestExtraCurrency {
  public String ecSwapAddress = "kQC_rkxBuZDwS81yvMSLzeXBNLCGFNofm0avwlMfNXCwoOgr";

  @Test
  public void testExtraCurrency() throws InterruptedException {

    Tonlib tonlib =
        Tonlib.builder()
            .testnet(true)
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .ignoreCache(false)
            .build();

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV3R2 contract = WalletV3R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress1 = contract.getAddress().toNonBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("    raw address: {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress1), Utils.toNano(4));
    log.info("topped up {}", Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForDeployment();

    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract.getSeqno())
            .destination(Address.of(ecSwapAddress))
            .amount(Utils.toNano(3.7))
            .build();

    //  receive test extra-currency (ECHIDNA)
    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForBalanceChange();

    Utils.sleep(30);

    FullAccountState accountState = tonlib.getAccountState(Address.of(rawAddress));
    log.info("state {}", accountState);
    RawAccountState rawAccountState = tonlib.getRawAccountState(Address.of(rawAddress));
    log.info("rawState {}", rawAccountState);

    RawTransactions txs = tonlib.getRawTransactions(rawAddress, null, null);
    for (RawTransaction tx : txs.getTransactions()) {
      log.info("tx {}", tx);
    }

    log.info(
        "new extra-currency balance {} ECHIDNA",
        Utils.formatJettonValue(rawAccountState.getExtra_currencies().get(0).getAmount(), 8, 2));

    // send back extra-currency to ecSwap address
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract.getSeqno())
            .destination(Address.of(ecSwapAddress))
            .amount(Utils.toNano(0.1))
            .extraCurrencies(
                Collections.singletonList(
                    ExtraCurrency.builder().id(100).amount(BigInteger.valueOf(600000000)).build()))
            .build();

    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForBalanceChange();

    Utils.sleep(30);
    accountState = tonlib.getAccountState(Address.of(rawAddress));
    log.info("state {}", accountState);
    rawAccountState = tonlib.getRawAccountState(Address.of(rawAddress));
    log.info("rawState {}", rawAccountState);
  }

  /**
   *
   *
   * <pre>
   * 1. deploys two wallets: wallet1 and wallet1
   * 2. wallet1 requests and receives extra-currency ECHIDNA from faucet
   * 3. wallet1 sends to wallet2 3 ECHIDNA with comment
   * 4. wallet2 waits for ECHIDNAS from wallet1 and checks if tx with EC has comment and bounce=false.
   * </pre>
   */
  @Test
  public void testExtraCurrencyMonitor() throws InterruptedException {

    Tonlib tonlib =
        Tonlib.builder().testnet(true).pathToTonlibSharedLib(Utils.getTonlibGithubUrl()).build();

    WalletV3R2 wallet1 = deployWallet(tonlib);
    WalletV3R2 wallet2 = deployWallet(tonlib);

    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(wallet1.getSeqno())
            .destination(Address.of(ecSwapAddress))
            .amount(Utils.toNano(3.7))
            .build();

    // send request to EC Swap faucet contract in order to receive test extra-currency (ECHIDNA)
    wallet1.sendWithConfirmation(config);

    long EC_ECHIDNA_ID = 100;
    wallet1.waitForExtraCurrency(EC_ECHIDNA_ID);

    log.info("send extra-currency from wallet1 to wallet2 with comment and bounce=false");
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(wallet1.getSeqno())
            .destination(wallet2.getAddress())
            .amount(BigInteger.ZERO)
            .comment("send ec wallet1->wallet2")
            .extraCurrencies(
                Collections.singletonList(
                    ExtraCurrency.builder().id(100).amount(BigInteger.valueOf(300000000)).build()))
            .build();

    wallet1.sendWithConfirmation(config);

    RawTransaction tx = wallet2.waitForExtraCurrency(EC_ECHIDNA_ID);
    log.info(
        "wallet2({}) received from wallet1({}) extra-currency with id {}, value {}, comment: {}",
        tx.getIn_msg().getDestination().getAccount_address(),
        tx.getIn_msg().getSource().getAccount_address(),
        tx.getFirstExtraCurrencyId(),
        tx.getFirstExtraCurrencyValue(),
        tx.getIn_msg().getComment());
  }

  private static WalletV3R2 deployWallet(Tonlib tonlib) throws InterruptedException {

    log.info("deploying new wallet...");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV3R2 wallet = WalletV3R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String rawAddress = wallet.getAddress().toRaw();

    log.info("         raw address: {}", rawAddress);
    log.info("nonBounceableAddress: {}", wallet.getAddress().toNonBounceableTestnet());
    log.info("bounceableAddress: {}", wallet.getAddress().toBounceableTestnet());

    // top up initial WalletV3R2 wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            tonlib, Address.of(wallet.getAddress().toNonBounceableTestnet()), Utils.toNano(4));
    log.info("topped up {}", Utils.formatNanoValue(balance));

    // deploy WalletV3R2 code
    ExtMessageInfo extMessageInfo = wallet.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();
    wallet.waitForDeployment();
    log.info("wallet {} deployed", rawAddress);
    return wallet;
  }

  /** send extra-currency to random 900 recipients */
  @Test
  public void testExtraCurrencyHighloadWalletV3() throws InterruptedException {

    Tonlib tonlib =
        Tonlib.builder()
            .testnet(true)
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .ignoreCache(false)
            .build();

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    HighloadWalletV3 contract =
        HighloadWalletV3.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress1 = contract.getAddress().toNonBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("    raw address: {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress1), Utils.toNano(4));
    log.info("topped up {}", Utils.formatNanoValue(balance));

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    ExtMessageInfo extMessageInfo = contract.deploy(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForDeployment();

    config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .body(
                contract.createBulkTransfer(
                    Collections.singletonList(
                        Destination.builder()
                            .address(ecSwapAddress)
                            .amount(Utils.toNano(3.7))
                            .build()),
                    BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
            .build();

    //  receive test extra-currency (ECHIDNA)
    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
    log.info("sent 1 message with request to get 3.7 ECHIDNA extra-currency");

    contract.waitForBalanceChange();

    Utils.sleep(30);

    FullAccountState accountState = tonlib.getAccountState(Address.of(rawAddress));
    log.info("state {}", accountState);
    RawAccountState rawAccountState = tonlib.getRawAccountState(Address.of(rawAddress));
    log.info("rawState {}", rawAccountState);

    RawTransactions txs = tonlib.getRawTransactions(rawAddress, null, null);
    for (RawTransaction tx : txs.getTransactions()) {
      log.info("tx {}", tx);
    }

    log.info(
        "new extra-currency balance {} ECHIDNA",
        Utils.formatJettonValue(rawAccountState.getExtra_currencies().get(0).getAmount(), 8, 2));

    // bulk send 0.001 ECHIDNA to random 900 addresses

    config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(2).getQueryId())
            .body(
                contract.createBulkTransfer(
                    createDummyDestinationsWithEc(900),
                    BigInteger.valueOf(HighloadQueryId.fromSeqno(2).getQueryId())))
            .build();

    extMessageInfo = contract.send(config);
    AssertionsForClassTypes.assertThat(extMessageInfo.getError().getCode()).isZero();
    log.info("sent 1000 messages");

    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForBalanceChange();

    Utils.sleep(30);
    accountState = tonlib.getAccountState(Address.of(rawAddress));
    log.info("state {}", accountState);
    rawAccountState = tonlib.getRawAccountState(Address.of(rawAddress));
    log.info("rawState {}", rawAccountState);
  }

  List<Destination> createDummyDestinationsWithEc(int count) {
    List<Destination> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String dstDummyAddress = Utils.generateRandomAddress(0);

      result.add(
          Destination.builder()
              .bounce(false)
              .address(dstDummyAddress)
              .amount(Utils.toNano(0.0025))
              .extraCurrencies(
                  Collections.singletonList(
                      ExtraCurrency.builder()
                          .id(100)
                          .amount(BigInteger.valueOf(100000))
                          .build())) // 0.001 ECHIDNA
              .build());
    }
    return result;
  }
}
