package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.WalletV1R2Config;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R2;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R2 extends CommonTest {

  @Test
  public void testNewWalletV1R2() throws InterruptedException {

    WalletV1R2 contract = WalletV1R2.builder().tonlib(tonlib).initialSeqno(2).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(45);

    log.info("wallet seqno: {}", contract.getSeqno());

    WalletV1R2Config config =
        WalletV1R2Config.builder()
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testNewWalletV1R2")
            .build();

    // transfer coins from new wallet (back to faucet)
    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForBalanceChange(45);

    balance = contract.getBalance();
    log.info("wallet {} new balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    log.info("wallet seqno: {}", contract.getSeqno());
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
  }

  @Test
  public void testWalletSignedExternally() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();

    WalletV1R2 contract = WalletV1R2.builder().tonlib(tonlib).publicKey(publicKey).build();
    log.info("pub key: {}", Utils.bytesToHex(publicKey));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, contract.getAddress(), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy using externally signed body
    Cell deployBody = contract.createDeployMessage();

    byte[] signedDeployBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), deployBody.hash());

    SendResponse sendResponse = contract.deploy(signedDeployBodyHash);
    log.info("extMessageInfo {}", sendResponse);
    contract.waitForDeployment(120);

    // send toncoins
    WalletV1R2Config config =
        WalletV1R2Config.builder()
            .seqno(1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testWalletV1R2-signed-externally")
            .build();

    // transfer coins from new wallet (back to faucet) using externally signed body
    Cell transferBody = contract.createTransferBody(config);
    byte[] signedTransferBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash());
    sendResponse = contract.send(config, signedTransferBodyHash);
    log.info("extMessageInfo: {}", sendResponse);
    contract.waitForBalanceChange();
    Assertions.assertThat(contract.getBalance()).isLessThan(Utils.toNano(0.03));
  }

  @Test
  public void testNewWalletV1R2AdnlClient() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();
    WalletV1R2 contract =
        WalletV1R2.builder().adnlLiteClient(adnlLiteClient).initialSeqno(2).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(45);

    log.info("wallet seqno: {}", contract.getSeqno());

    WalletV1R2Config config =
        WalletV1R2Config.builder()
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testNewWalletV1R2")
            .build();

    // transfer coins from new wallet (back to faucet)
    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForBalanceChange(45);

    balance = contract.getBalance();
    log.info("wallet {} new balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    log.info("wallet seqno: {}", contract.getSeqno());
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
  }

  @Test
  public void testNewWalletV1R2AdnlClientWithConfirmation() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();
    WalletV1R2 contract =
        WalletV1R2.builder().adnlLiteClient(adnlLiteClient).initialSeqno(2).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(45);

    log.info("wallet seqno: {}", contract.getSeqno());

    WalletV1R2Config config =
        WalletV1R2Config.builder()
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testNewWalletV1R2")
            .build();

    // transfer coins from new wallet (back to faucet)
    contract.sendWithConfirmation(contract.prepareExternalMsg(config));

    balance = contract.getBalance();
    log.info("wallet {} new balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    log.info("wallet seqno: {}", contract.getSeqno());
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
  }

  @Test
  public void testNewWalletV1R2TonCenterClient() throws Exception {
    TonCenter tonCenterClient = TonCenter.builder().testnet().build();
    WalletV1R2 contract =
        WalletV1R2.builder().tonCenterClient(tonCenterClient).initialSeqno(2).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            tonCenterClient, Address.of(nonBounceableAddress), Utils.toNano(0.1), true);
    log.info("wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(45);

    log.info("wallet seqno: {}", contract.getSeqno());

    WalletV1R2Config config =
        WalletV1R2Config.builder()
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testNewWalletV1R2")
            .build();

    // transfer coins from new wallet (back to faucet)
    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForBalanceChange(45);

    balance = contract.getBalance();
    log.info("wallet {} new balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    log.info("wallet seqno: {}", contract.getSeqno());
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
  }

  @Test
  public void testNewWalletV1R2TonCenterClientWithConfirmation() throws Exception {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    TonCenter tonCenterClient = TonCenter.builder().apiKey(TESTNET_API_KEY).testnet().build();

    WalletV1R2 contract =
        WalletV1R2.builder().tonCenterClient(tonCenterClient).initialSeqno(2).keyPair(keyPair).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            tonCenterClient, Address.of(nonBounceableAddress), Utils.toNano(1), true);
    log.info("wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment();

    long seqno = contract.getSeqno();

    log.info("wallet seqno: {}", seqno);

    WalletV1R2Config config =
        WalletV1R2Config.builder()
            .seqno(seqno)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.8))
            .comment("ton4j testNewWalletV1R2")
            .build();

    // transfer coins from new wallet (back to faucet)
    contract.sendWithConfirmation(contract.prepareExternalMsg(config));

    balance = contract.getBalance();
    log.info("wallet {} new balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    log.info("wallet seqno: {}", contract.getSeqno());
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.3).longValue());
  }
}
