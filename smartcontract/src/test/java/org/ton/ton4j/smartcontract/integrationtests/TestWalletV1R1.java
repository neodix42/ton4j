package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.WalletV1R1Config;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R1;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.RawAccountState;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R1 extends CommonTest {

  @Test
  public void testNewWalletV1R1AutoKeyPair() throws InterruptedException {

    WalletV1R1 contract = WalletV1R1.builder().wc(0).tonlib(tonlib).build();

    Address walletAddress = contract.getAddress();

    log.info("Wallet version {}", contract.getName());
    log.info("Wallet pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("Wallet address {}", walletAddress);

    // top up new wallet using test-faucet-wallet
    BigInteger balance = TestnetFaucet.topUpContract(tonlib, walletAddress, Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForDeployment(45);

    RawAccountState accountState2 = tonlib.getRawAccountState(walletAddress);
    log.info("raw  accountState {}", accountState2);
    log.info("deployed? {}", contract.isDeployed());

    WalletV1R1Config config =
        WalletV1R1Config.builder()
            .seqno(1) // V1R1 does not have get_seqno method
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testNewWalletV1R1")
            .build();

    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();
  }

  @Test
  public void testNewWalletV1R1() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV1R1 contract = WalletV1R1.builder().tonlib(tonlib).wc(0).keyPair(keyPair).build();

    log.info("Wallet version {}", contract.getName());
    log.info("Wallet pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForDeployment(45);

    balance = contract.getBalance();
    log.info("    wallet balance: {}", Utils.formatNanoValue(balance));

    WalletV1R1Config config =
        WalletV1R1Config.builder()
            .seqno(1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testNewWalletV1R1")
            .build();

    // transfer coins from new wallet (back to faucet)
    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForBalanceChange(45);

    balance = contract.getBalance();
    log.info("new wallet balance: {}", Utils.formatNanoValue(balance));

    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
  }

  @Test
  public void testWalletSignedExternally() throws Exception {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();

    WalletV1R1 contract = WalletV1R1.builder().tonlib(tonlib).publicKey(publicKey).build();
    log.info("pub key: {}", Utils.bytesToHex(publicKey));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, contract.getAddress(), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy using externally signed body
    Cell deployBody = contract.createDeployMessage();

    byte[] signedDeployBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), deployBody.hash());

    ExtMessageInfo extMessageInfo = contract.deploy(signedDeployBodyHash);
    log.info("extMessageInfo {}", extMessageInfo);
    contract.waitForDeployment(120);

    // send toncoins
    WalletV1R1Config config =
        WalletV1R1Config.builder()
            .seqno(1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testWalletV1R1-signed-externally")
            .build();

    // transfer coins from new wallet (back to faucet) using externally signed body
    Cell transferBody = contract.createTransferBody(config);
    byte[] signedTransferBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash());
    extMessageInfo = contract.send(config, signedTransferBodyHash);
    log.info("extMessageInfo: {}", extMessageInfo);
    contract.waitForBalanceChange(120);
    Assertions.assertThat(contract.getBalance()).isLessThan(Utils.toNano(0.03));
  }

  @Test
  public void testNewWalletV1R1AdnlClient() throws Exception {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();

    WalletV1R1 contract =
        WalletV1R1.builder().adnlLiteClient(adnlLiteClient).wc(0).keyPair(keyPair).build();

    log.info("Wallet version {}", contract.getName());
    log.info("Wallet pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForDeployment(45);

    balance = contract.getBalance();
    log.info("    wallet balance: {}", Utils.formatNanoValue(balance));

    WalletV1R1Config config =
        WalletV1R1Config.builder()
            .seqno(1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testNewWalletV1R1")
            .build();

    // transfer coins from new wallet (back to faucet)
    extMessageInfo = contract.send(contract.prepareExternalMsg(config));
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForBalanceChange(45);

    balance = contract.getBalance();
    log.info("new wallet balance: {}", Utils.formatNanoValue(balance));

    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
  }

  @Test
  public void testNewWalletV1R1TonCenterClient() throws Exception {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    TonCenter tonCenterClient =
        TonCenter.builder()
            .apiKey(TESTNET_API_KEY)
            .testnet()
            .debug()
            .build();

    WalletV1R1 contract =
        WalletV1R1.builder().tonCenterClient(tonCenterClient).wc(0).keyPair(keyPair).build();

    log.info("Wallet version {}", contract.getName());
    log.info("Wallet pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            tonCenterClient, Address.of(nonBounceableAddress), Utils.toNano(0.1), true);
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getTonCenterError().getCode()).isZero();
    contract.waitForDeployment(45);

    balance = contract.getBalance();
    log.info("    wallet balance: {}", Utils.formatNanoValue(balance));

    WalletV1R1Config config =
        WalletV1R1Config.builder()
            .seqno(1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("ton4j testNewWalletV1R1")
            .build();

    // transfer coins from new wallet (back to faucet)
    extMessageInfo = contract.send(contract.prepareExternalMsg(config));
    assertThat(extMessageInfo.getTonCenterError().getCode()).isZero();

    contract.waitForBalanceChange();

    balance = contract.getBalance();
    log.info("new wallet balance: {}", Utils.formatNanoValue(balance));

    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.03).longValue());
  }
}
