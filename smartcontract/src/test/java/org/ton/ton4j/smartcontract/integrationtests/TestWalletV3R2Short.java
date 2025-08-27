package org.ton.ton4j.smartcontract.integrationtests;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.highload.HighloadWallet;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.tl.liteserver.responses.TransactionList;
import org.ton.ton4j.tlb.Message;
import org.ton.ton4j.tlb.Transaction;
import org.ton.ton4j.toncenter.Network;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.model.TransactionResponse;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RawMessage;
import org.ton.ton4j.tonlib.types.RawTransaction;
import org.ton.ton4j.tonlib.types.RawTransactions;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV3R2Short extends CommonTest {

  @Test
  public void testWalletV3R12() {
    String hex = Utils.base64ToHexString("dGVzdFdhbGxldFYzUjItOTg=");
    String str = CellSlice.beginParse(Cell.fromHex(hex)).loadSnakeString();
    log.info("cell {}", str);
  }

  @Test
  public void testWalletV3R2() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV3R2 contract1 =
        WalletV3R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress1 = contract1.getAddress().toNonBounceable();
    String bounceableAddress1 = contract1.getAddress().toBounceable();
    String rawAddress1 = contract1.getAddress().toRaw();

    log.info("non-bounceable address 1: {}", nonBounceableAddress1);
    log.info("    bounceable address 1: {}", bounceableAddress1);
    log.info("    raw address 1: {}", rawAddress1);
    log.info("pub-key {}", Utils.bytesToHex(contract1.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract1.getKeyPair().getSecretKey()));

    String status = tonlib.getAccountStatus(Address.of(bounceableAddress1));
    log.info("account status {}", status);

    WalletV3R2 contract2 =
        WalletV3R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(98).build();

    String nonBounceableAddress2 = contract2.getAddress().toNonBounceable();
    String bounceableAddress2 = contract2.getAddress().toBounceable();
    String rawAddress2 = contract2.getAddress().toRaw();

    log.info("non-bounceable address 2: {}", nonBounceableAddress2);
    log.info("    bounceable address 2: {}", bounceableAddress2);
    log.info("    raw address 2: {}", rawAddress2);

    log.info("pub-key {}", Utils.bytesToHex(contract2.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract2.getKeyPair().getSecretKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance1 =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress1), Utils.toNano(1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract1.getWalletId(),
        contract1.getName(),
        Utils.formatNanoValue(balance1));

    BigInteger balance2 =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress2), Utils.toNano(1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract2.getWalletId(),
        contract2.getName(),
        Utils.formatNanoValue(balance2));

    SendResponse sendResponse = contract1.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract1.waitForDeployment(30);

    sendResponse = contract2.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract2.waitForDeployment(30);

    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract1.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.8))
            .comment("testWalletV3R2-42")
            .build();

    // transfer coins from new wallet (back to faucet)
    sendResponse = contract1.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract1.waitForBalanceChange(90);

    config =
        WalletV3Config.builder()
            .walletId(98)
            .seqno(contract2.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.7))
            .comment("testWalletV3R2-98")
            .build();

    sendResponse = contract2.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract2.waitForBalanceChange(90);

    balance1 = contract1.getBalance();
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract1.getWalletId(),
        contract1.getName(),
        Utils.formatNanoValue(balance1));

    balance2 = contract2.getBalance();
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract2.getWalletId(),
        contract2.getName(),
        Utils.formatNanoValue(balance2));

    log.info("1 seqno {}", contract1.getSeqno());
    log.info("1 pubkey {}", contract1.getPublicKey());

    log.info("2 seqno {}", contract2.getSeqno());
    log.info("2 pubkey {}", contract2.getPublicKey());

    assertThat(contract1.getPublicKey()).isEqualTo(contract2.getPublicKey());

    log.info("txs of wallet1");
    RawTransactions txs = tonlib.getRawTransactions(bounceableAddress1, null, null);
    for (RawTransaction tx : txs.getTransactions()) {
      if (nonNull(tx.getIn_msg())
          && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
        log.info(
            "{}, {} <<<<< {} : {}, comment: {} ",
            Utils.toUTC(tx.getUtime()),
            tx.getIn_msg().getSource().getAccount_address(),
            tx.getIn_msg().getDestination().getAccount_address(),
            Utils.formatNanoValue(tx.getIn_msg().getValue()),
            tx.getIn_msg().getComment());
      }
      if (nonNull(tx.getOut_msgs())) {
        for (RawMessage msg : tx.getOut_msgs()) {
          log.info(
              "{}, {} >>>>> {} : {}, comment: {}",
              Utils.toUTC(tx.getUtime()),
              msg.getSource().getAccount_address(),
              msg.getDestination().getAccount_address(),
              Utils.formatNanoValue(msg.getValue()),
              msg.getComment());
        }
      }
    }
    log.info("txs of wallet2");
    txs = tonlib.getRawTransactions(bounceableAddress2, null, null);
    for (RawTransaction tx : txs.getTransactions()) {
      if (nonNull(tx.getIn_msg())
          && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
        log.info(
            "{}, {} <<<<< {} : {}, comment: {} ",
            Utils.toUTC(tx.getUtime()),
            tx.getIn_msg().getSource().getAccount_address(),
            tx.getIn_msg().getDestination().getAccount_address(),
            Utils.formatNanoValue(tx.getIn_msg().getValue()),
            tx.getIn_msg().getComment());
      }
      if (nonNull(tx.getOut_msgs())) {
        for (RawMessage msg : tx.getOut_msgs()) {
          log.info(
              "{}, {} >>>>> {} : {}, comment: {}",
              Utils.toUTC(tx.getUtime()),
              msg.getSource().getAccount_address(),
              msg.getDestination().getAccount_address(),
              Utils.formatNanoValue(msg.getValue()),
              msg.getComment());
        }
      }
    }

    log.info("txs of wallet1");
    tonlib.printAccountTransactions(contract1.getAddress());
    log.info("msgs of wallet2");
    tonlib.printAccountMessages(contract2.getAddress());
  }

  /*
   * addr - EQA-XwAkPLS-i4s9_N5v0CXGVFecw7lZV2rYeXDAimuWi9zI
   * pub key - 2c188d86ba469755554baad436663b8073145b29f117550432426c513e7c582a
   * prv key - c67cf48806f08929a49416ebebd97078100540ac8a3283646222b4d958b3e9e22c188d86ba469755554baad436663b8073145b29f117550432426c513e7c582a
   */
  @Test
  public void testWallet() throws InterruptedException {
    WalletV3R2 contract = WalletV3R2.builder().tonlib(tonlib).walletId(42).build();
    log.info("pub key: {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv key: {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, contract.getAddress(), Utils.toNano(1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract.getWalletId(),
        contract.getName(),
        Utils.formatNanoValue(balance));

    contract.deploy();
    contract.waitForDeployment(60);
  }

  @Test
  public void testWalletV3R2MyLocalTonDockerFaucet() {

    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .pathToGlobalConfig("http://127.0.0.1:8000/localhost.global.config.json")
            .ignoreCache(false)
            .build();

    log.info("last {}", tonlib.getLast());

    byte[] prvKey =
        Utils.hexToSignedBytes("249489b5c1bfa6f62451be3714679581ee04cc8f82a8e3f74b432a58f3e4fedf");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(prvKey);

    WalletV3R2 contract =
        WalletV3R2.builder().tonlib(tonlib).wc(-1).keyPair(keyPair).walletId(42).build();
    log.info("WalletV3R2 address {}", contract.getAddress().toRaw());
    assertThat(contract.getAddress().toRaw())
        .isEqualTo("-1:db7ef76c48e888b7a35d3c88ed61cc33e2ec84b74f0ce2d159e4dd6cd34f406c");
  }

  @Test
  public void testWalletV3R2MyLocalTonDockerFaucetHighLoad() {

    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .pathToGlobalConfig("http://127.0.0.1:8000/localhost.global.config.json")
            .ignoreCache(false)
            .build();

    log.info("last {}", tonlib.getLast());

    byte[] prvKey =
        Utils.hexToSignedBytes("ee26cd8f2709404b63bc172148ec6179bfc7049b1045a22c3ea5446c5d425347");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(prvKey);

    HighloadWallet contract =
        HighloadWallet.builder()
            .tonlib(tonlib)
            .wc(-1)
            .keyPair(keyPair)
            .walletId(42)
            .queryId(BigInteger.ZERO)
            .build();

    assertThat(contract.getAddress().toRaw())
        .isEqualTo("-1:fee48a6002da9ad21c61a6a2e4dd73c005d46101450b52bf47d1ce16cdc8230f");
  }

  @Test
  public void testWalletV3R2MyLocalTonDockerValidator1() {

    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .pathToGlobalConfig("http://127.0.0.1:8000/localhost.global.config.json")
            .ignoreCache(false)
            .build();

    log.info("last {}", tonlib.getLast());

    byte[] prvKey =
        Utils.hexToSignedBytes("001624080b055bf5ea72a252c1acc2c18552df27b4073a412fbde398d8061316");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(prvKey);

    WalletV3R2 contract =
        WalletV3R2.builder().tonlib(tonlib).wc(-1).keyPair(keyPair).walletId(42).build();
    assertThat(contract.getAddress().toRaw())
        .isEqualTo("-1:0e4160632db47d34bad8a24b55a56f46ca3b6fc84826d90515cd2b6313bd7cf6");
  }

  @Test
  public void testWalletV3R2MyLocalTonDockerValidator2() {

    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .pathToGlobalConfig("http://127.0.0.1:8000/localhost.global.config.json")
            .ignoreCache(false)
            .build();

    log.info("last {}", tonlib.getLast());

    byte[] prvKey =
        Utils.hexToSignedBytes("1da5f8b57104cc6c8af748c0541abc8a735362cd241aa96c201d696623684672");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(prvKey);

    WalletV3R2 contract =
        WalletV3R2.builder().tonlib(tonlib).wc(-1).keyPair(keyPair).walletId(42).build();
    assertThat(contract.getAddress().toRaw())
        .isEqualTo("-1:ddd8df36e13e3bcec0ffbcfb4de51535d39937311b3c5cad520a0802d3de9b54");
  }

  @Test
  public void testWalletV3R2MyLocalTonDockerValidator3() {

    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .pathToGlobalConfig("http://127.0.0.1:8000/localhost.global.config.json")
            .ignoreCache(false)
            .build();

    log.info("last {}", tonlib.getLast());

    byte[] prvKey =
        Utils.hexToSignedBytes("fe968161dfe5aa6d7a6f8fdd1d43ceeee9395f1ca61bb8224d4f60e48fdc589d");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(prvKey);

    WalletV3R2 contract =
        WalletV3R2.builder().tonlib(tonlib).wc(-1).keyPair(keyPair).walletId(42).build();
    assertThat(contract.getAddress().toRaw())
        .isEqualTo("-1:1ea99012e00cee2aef95c6ac245ee28894080801e4e5fae2d91363f2ef5a7232");
  }

  @Test
  public void testWalletV3R2MyLocalTonDockerValidator4() {

    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .pathToGlobalConfig("http://127.0.0.1:8000/localhost.global.config.json")
            .ignoreCache(false)
            .build();

    log.info("last {}", tonlib.getLast());

    byte[] prvKey =
        Utils.hexToSignedBytes("49cce23987cacbd05fac13978eff826e9107d694c0040a1e98bca4c2872d80f8");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(prvKey);

    WalletV3R2 contract =
        WalletV3R2.builder().tonlib(tonlib).wc(-1).keyPair(keyPair).walletId(42).build();
    assertThat(contract.getAddress().toRaw())
        .isEqualTo("-1:c21b6e9f20c35f31a3c46e510daae29261c3127e43aa2c90e5d1463451f623f8");
  }

  @Test
  public void testWalletV3R2MyLocalTonDockerValidator5() {

    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .pathToGlobalConfig("http://127.0.0.1:8000/localhost.global.config.json")
            .ignoreCache(false)
            .build();

    log.info("last {}", tonlib.getLast());

    byte[] prvKey =
        Utils.hexToSignedBytes("b5e0ce4fba8ae2e3f44a393ac380549bfa44c3a5ba33a49171d502f1e4ac6c1d");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(prvKey);

    WalletV3R2 contract =
        WalletV3R2.builder().tonlib(tonlib).wc(-1).keyPair(keyPair).walletId(42).build();
    assertThat(contract.getAddress().toRaw())
        .isEqualTo("-1:a485d0e84de33e212d66eb025fbbaecbeed9dbad7f78cd8cd2058afe20cebde9");
  }

  @Test
  public void testWalletV3R2MyLocalTonDockerGenesis() {

    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .pathToGlobalConfig("http://127.0.0.1:8000/localhost.global.config.json")
            .ignoreCache(false)
            .build();

    log.info("last {}", tonlib.getLast());

    byte[] prvKey =
        Utils.hexToSignedBytes("5f14ebefc57461002fc07f9438a63ad35ff609759bb0ae334fedabbfb4bfdce8");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(prvKey);

    WalletV3R2 contract =
        WalletV3R2.builder().tonlib(tonlib).wc(-1).keyPair(keyPair).walletId(42).build();
    assertThat(contract.getAddress().toRaw())
        .isEqualTo("-1:0755526dfc926d1b6d468801099cad2d588f40a6a6088bcd3e059566c0ef907c");
  }

  @Test
  public void testWalletSignedExternally() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();

    WalletV3R2 contract =
        WalletV3R2.builder().tonlib(tonlib).publicKey(publicKey).walletId(42).build();
    log.info("pub key: {}", Utils.bytesToHex(publicKey));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, contract.getAddress(), Utils.toNano(0.1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract.getWalletId(),
        contract.getName(),
        Utils.formatNanoValue(balance));

    // deploy using externally signed body
    Cell deployBody = contract.createDeployMessage();

    byte[] signedDeployBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), deployBody.hash());

    contract.deploy(signedDeployBodyHash);
    contract.waitForDeployment(60);

    // send toncoins
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testWalletV3R2-signed-externally")
            .build();

    // transfer coins from new wallet (back to faucet) using externally signed body
    Cell transferBody = contract.createTransferBody(config);
    byte[] signedTransferBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash());
    SendResponse sendResponse = contract.send(config, signedTransferBodyHash);
    log.info("extMessageInfo: {}", sendResponse);
    contract.waitForBalanceChange();
    assertThat(contract.getBalance()).isLessThan(Utils.toNano(0.03));
  }

  @Test
  public void testWalletV3R2AdnlLiteClient() throws Exception {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();
    WalletV3R2 contract1 =
        WalletV3R2.builder().adnlLiteClient(adnlLiteClient).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress1 = contract1.getAddress().toNonBounceable();
    String bounceableAddress1 = contract1.getAddress().toBounceable();
    String rawAddress1 = contract1.getAddress().toRaw();

    log.info("non-bounceable address 1: {}", nonBounceableAddress1);
    log.info("    bounceable address 1: {}", bounceableAddress1);
    log.info("    raw address 1: {}", rawAddress1);
    log.info("pub-key {}", Utils.bytesToHex(contract1.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract1.getKeyPair().getSecretKey()));

    WalletV3R2 contract2 =
        WalletV3R2.builder().adnlLiteClient(adnlLiteClient).keyPair(keyPair).walletId(98).build();

    String nonBounceableAddress2 = contract2.getAddress().toNonBounceable();
    String bounceableAddress2 = contract2.getAddress().toBounceable();
    String rawAddress2 = contract2.getAddress().toRaw();

    log.info("non-bounceable address 2: {}", nonBounceableAddress2);
    log.info("    bounceable address 2: {}", bounceableAddress2);
    log.info("    raw address 2: {}", rawAddress2);

    log.info("pub-key {}", Utils.bytesToHex(contract2.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract2.getKeyPair().getSecretKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance1 =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(nonBounceableAddress1), Utils.toNano(1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract1.getWalletId(),
        contract1.getName(),
        Utils.formatNanoValue(balance1));

    BigInteger balance2 =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(nonBounceableAddress2), Utils.toNano(1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract2.getWalletId(),
        contract2.getName(),
        Utils.formatNanoValue(balance2));

    SendResponse sendResponse = contract1.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract1.waitForDeployment(30);

    sendResponse = contract2.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract2.waitForDeployment(30);

    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract1.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.8))
            .comment("testWalletV3R2-42")
            .build();

    // transfer coins from new wallet (back to faucet)
    sendResponse = contract1.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract1.waitForBalanceChange(90);

    config =
        WalletV3Config.builder()
            .walletId(98)
            .seqno(contract2.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.7))
            .comment("testWalletV3R2-98")
            .build();

    sendResponse = contract2.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract2.waitForBalanceChange(90);

    balance1 = contract1.getBalance();
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract1.getWalletId(),
        contract1.getName(),
        Utils.formatNanoValue(balance1));

    balance2 = contract2.getBalance();
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract2.getWalletId(),
        contract2.getName(),
        Utils.formatNanoValue(balance2));

    log.info("1 seqno {}", contract1.getSeqno());
    log.info("1 pubkey {}", contract1.getPublicKey());

    log.info("2 seqno {}", contract2.getSeqno());
    log.info("2 pubkey {}", contract2.getPublicKey());

    assertThat(contract1.getPublicKey()).isEqualTo(contract2.getPublicKey());

    log.info("txs of wallet1");
    TransactionList txs = adnlLiteClient.getTransactions(contract1.getAddress(), 0, null, 100);
    for (Transaction tx : txs.getTransactionsParsed()) {
      if (nonNull(tx.getInOut().getIn())
          && StringUtils.isNotEmpty(tx.getInOut().getIn().getInfo().getSourceAddress())) {
        log.info(
            "{}, {} <<<<< {} : {}, comment: {} ",
            Utils.toUTC(tx.getNow()),
            tx.getInOut().getIn().getInfo().getSourceAddress(),
            tx.getInOut().getIn().getInfo().getDestinationAddress(),
            Utils.formatNanoValue(tx.getInOut().getIn().getInfo().getValueCoins()),
            tx.getInOut().getIn().getComment());
      }
      if (nonNull(tx.getInOut().getOut())) {
        for (Message msg : tx.getInOut().getOutMessages()) {
          log.info(
              "{}, {} >>>>> {} : {}, comment: {}",
              Utils.toUTC(tx.getNow()),
              msg.getInfo().getSourceAddress(),
              msg.getInfo().getDestinationAddress(),
              Utils.formatNanoValue(msg.getInfo().getValueCoins()),
              msg.getComment());
        }
      }
    }

    log.info("txs of wallet2");
    txs = adnlLiteClient.getTransactions(contract2.getAddress(), 0, null, 100);
    for (Transaction tx : txs.getTransactionsParsed()) {
      if (nonNull(tx.getInOut().getIn())
          && StringUtils.isNotEmpty(tx.getInOut().getIn().getInfo().getSourceAddress())) {
        log.info(
            "{}, {} <<<<< {} : {}, comment: {} ",
            Utils.toUTC(tx.getNow()),
            tx.getInOut().getIn().getInfo().getSourceAddress(),
            tx.getInOut().getIn().getInfo().getDestinationAddress(),
            Utils.formatNanoValue(tx.getInOut().getIn().getInfo().getValueCoins()),
            tx.getInOut().getIn().getComment());
      }
      if (nonNull(tx.getInOut().getOut())) {
        for (Message msg : tx.getInOut().getOutMessages()) {
          log.info(
              "{}, {} >>>>> {} : {}, comment: {}",
              Utils.toUTC(tx.getNow()),
              msg.getInfo().getSourceAddress(),
              msg.getInfo().getDestinationAddress(),
              Utils.formatNanoValue(msg.getInfo().getValueCoins()),
              msg.getComment());
        }
      }
    }
    contract1.printTransactions();
    contract2.printMessages();
  }

  @Test
  public void testWalletV3R2TonCenterClient() throws Exception {

    TonCenter toncenter =
        TonCenter.builder().apiKey(TESTNET_API_KEY).network(Network.TESTNET).build();

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV3R2 contract1 =
        WalletV3R2.builder().keyPair(keyPair).tonCenterClient(toncenter).walletId(42).build();

    String nonBounceableAddress1 = contract1.getAddress().toNonBounceable();
    String bounceableAddress1 = contract1.getAddress().toBounceable();
    String rawAddress1 = contract1.getAddress().toRaw();

    log.info("non-bounceable address 1: {}", nonBounceableAddress1);
    log.info("    bounceable address 1: {}", bounceableAddress1);
    log.info("    raw address 1: {}", rawAddress1);
    log.info("pub-key 1 {}", Utils.bytesToHex(contract1.getKeyPair().getPublicKey()));
    log.info("prv-key 1 {}", Utils.bytesToHex(contract1.getKeyPair().getSecretKey()));

    WalletV3R2 contract2 =
        WalletV3R2.builder().keyPair(keyPair).tonCenterClient(toncenter).walletId(98).build();

    String nonBounceableAddress2 = contract2.getAddress().toNonBounceable();
    String bounceableAddress2 = contract2.getAddress().toBounceable();
    String rawAddress2 = contract2.getAddress().toRaw();

    log.info("non-bounceable address 2: {}", nonBounceableAddress2);
    log.info("    bounceable address 2: {}", bounceableAddress2);
    log.info("    raw address 2: {}", rawAddress2);

    log.info("pub-key 2 {}", Utils.bytesToHex(contract2.getKeyPair().getPublicKey()));
    log.info("prv-key 2 {}", Utils.bytesToHex(contract2.getKeyPair().getSecretKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance1 =
        TestnetFaucet.topUpContract(
            toncenter, Address.of(nonBounceableAddress1), Utils.toNano(1), true);
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract1.getWalletId(),
        contract1.getName(),
        Utils.formatNanoValue(balance1));

    BigInteger balance2 =
        TestnetFaucet.topUpContract(
            toncenter, Address.of(nonBounceableAddress2), Utils.toNano(1), true);
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract2.getWalletId(),
        contract2.getName(),
        Utils.formatNanoValue(balance2));

    SendResponse response = contract1.deploy();
    assertThat(response.getCode()).isZero();
    contract1.waitForDeployment();

    response = contract2.deploy();
    assertThat(response.getCode()).isZero();
    contract2.waitForDeployment();

    log.info("both contracts should be deployed");

    long seqno1 = toncenter.getSeqno(contract1.getAddress().toBounceable());
    log.info("contract1 seqno {}", seqno1);

    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(seqno1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.8))
            .comment("ton4j testWalletV3R2-42")
            .build();

    Utils.sleep(2);
    // transfer coins from new wallet (back to faucet)
    response = contract1.send(config);
    assertThat(response.getCode()).isZero();
    contract1.waitForBalanceChangeWithTolerance(30, Utils.toNano(0.5));

    long seqno2 = toncenter.getSeqno(contract2.getAddress().toBounceable());
    log.info("contract2 seqno {}", seqno2);

    Utils.sleep(2);
    config =
        WalletV3Config.builder()
            .walletId(98)
            .seqno(seqno2)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.7))
            .comment("ton4j testWalletV3R2-98")
            .build();

    contract2.sendWithConfirmation(contract2.prepareExternalMsg(config));
    //    assertThat(response.getCode()).isZero();
    //    contract2.waitForBalanceChangeWithTolerance(30, Utils.toNano(0.5));

    balance1 = contract1.getBalance();
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract1.getWalletId(),
        contract1.getName(),
        Utils.formatNanoValue(balance1));

    Utils.sleep(2);

    balance2 = contract2.getBalance();
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract2.getWalletId(),
        contract2.getName(),
        Utils.formatNanoValue(balance2));

    Utils.sleep(2);
    log.info("1 seqno {}", toncenter.getSeqno(contract1.getAddress().toBounceable()));
    Utils.sleep(2);
    BigInteger pubKey1 = toncenter.getPublicKey(contract1.getAddress().toBounceable());
    log.info("1 pubkey {}", pubKey1);
    Utils.sleep(2);
    log.info("2 seqno {}", toncenter.getSeqno(contract2.getAddress().toBounceable()));
    Utils.sleep(2);
    BigInteger pubKey2 = toncenter.getPublicKey(contract2.getAddress().toBounceable());
    log.info("2 pubkey {}", pubKey2);

    Utils.sleep(2);
    assertThat(pubKey1).isEqualTo(pubKey2);

    Utils.sleep(2);
    log.info("txs of wallet1");
    List<TransactionResponse> responseTxs =
        toncenter.getTransactions(contract1.getAddress().toBounceable(), 100).getResult();
    for (TransactionResponse tx : responseTxs) {
      if (nonNull(tx.getInMsg()) && StringUtils.isNotEmpty(tx.getInMsg().getSource())) {
        log.info(
            "{}, {} <<<<< {} : {}, comment: {} ",
            Utils.toUTC(tx.getUtime()),
            tx.getInMsg().getSource(),
            tx.getInMsg().getDestination(),
            Utils.formatNanoValue(tx.getInMsg().getValue()),
            tx.getInMsg().getMessage());
      }
      if (nonNull(tx.getOutMsgs())) {
        for (TransactionResponse.Message msg : tx.getOutMsgs()) {
          log.info(
              "{}, {} >>>>> {} : {}, comment: {}",
              Utils.toUTC(tx.getUtime()),
              msg.getSource(),
              msg.getDestination(),
              Utils.formatNanoValue(msg.getValue()),
              msg.getMessage());
        }
      }
    }

    Utils.sleep(2);
    log.info("txs of wallet2");
    responseTxs = toncenter.getTransactions(contract2.getAddress().toBounceable(), 100).getResult();
    for (TransactionResponse tx : responseTxs) {
      if (nonNull(tx.getInMsg()) && StringUtils.isNotEmpty(tx.getInMsg().getSource())) {
        log.info(
            "{}, {} <<<<< {} : {}, comment: {} ",
            Utils.toUTC(tx.getUtime()),
            tx.getInMsg().getSource(),
            tx.getInMsg().getDestination(),
            Utils.formatNanoValue(tx.getInMsg().getValue()),
            tx.getInMsg().getMessage());
      }
      if (nonNull(tx.getOutMsgs())) {
        for (TransactionResponse.Message msg : tx.getOutMsgs()) {
          log.info(
              "{}, {} >>>>> {} : {}, comment: {}",
              Utils.toUTC(tx.getUtime()),
              msg.getSource(),
              msg.getDestination(),
              Utils.formatNanoValue(msg.getValue()),
              msg.getMessage());
        }
      }
    }

    contract1.printTransactions(true);
    contract2.printMessages();
  }
}
