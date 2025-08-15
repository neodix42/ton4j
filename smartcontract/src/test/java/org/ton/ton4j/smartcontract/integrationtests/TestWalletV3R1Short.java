package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R1;
import org.ton.ton4j.toncenter.Network;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class TestWalletV3R1Short extends CommonTest {

  /*
   * addr - EQA-XwAkPLS-i4s9_N5v0CXGVFecw7lZV2rYeXDAimuWi9zI
   * pub key - 2c188d86ba469755554baad436663b8073145b29f117550432426c513e7c582a
   * prv key - c67cf48806f08929a49416ebebd97078100540ac8a3283646222b4d958b3e9e22c188d86ba469755554baad436663b8073145b29f117550432426c513e7c582a
   */
  @Test
  public void testWalletV3R1() throws InterruptedException {
    WalletV3R1 contract = WalletV3R1.builder().tonlib(tonlib).walletId(42).build();
    log.info("pub key: {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv key: {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, contract.getAddress(), Utils.toNano(0.1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract.getWalletId(),
        contract.getName(),
        Utils.formatNanoValue(balance));

    SendResponse sendResponse = contract.deploy();
    log.info(sendResponse.toString());
    contract.waitForDeployment(60);
    // send toncoins
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .amount(Utils.toNano(0.08))
            .comment("testWalletV3R1")
            .build();
    sendResponse = contract.send(config);
    log.info("sendResponse {}", sendResponse);
  }

  @Test
  public void testWalletSignedExternally() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();

    WalletV3R1 contract =
        WalletV3R1.builder().tonlib(tonlib).publicKey(publicKey).walletId(42).build();
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

    SendResponse sendResponse = contract.deploy(signedDeployBodyHash);
    log.info("sendResponse {}", sendResponse);
    contract.waitForDeployment(120);

    // send toncoins
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testWalletV3R1-signed-externally")
            .build();

    // transfer coins from new wallet (back to faucet) using externally signed body
    Cell transferBody = contract.createTransferBody(config);
    byte[] signedTransferBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash());
    sendResponse = contract.send(config, signedTransferBodyHash);
    log.info("sendResponse: {}", sendResponse);
    contract.waitForBalanceChange(120);
    assertThat(contract.getBalance()).isLessThan(Utils.toNano(0.03));
  }

  @Test
  public void testWalletV3R1AdnlLiteClient() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();
    WalletV3R1 contract = WalletV3R1.builder().adnlLiteClient(adnlLiteClient).walletId(42).build();
    log.info("pub key: {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv key: {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(adnlLiteClient, contract.getAddress(), Utils.toNano(0.1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract.getWalletId(),
        contract.getName(),
        Utils.formatNanoValue(balance));

    SendResponse sendResponse = contract.deploy();
    log.info("sendResponse {}", sendResponse);
    contract.waitForDeployment(60);
    // send toncoins
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .amount(Utils.toNano(0.08))
            .comment("testWalletV3R1")
            .build();
    sendResponse = contract.send(config);
    log.info("sendResponse {}", sendResponse);
  }

  @Test
  public void testWalletV3R1TonCenterClient() throws Exception {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    TonCenter tonCenterClient =
        TonCenter.builder().apiKey(TESTNET_API_KEY).network(Network.TESTNET).build();

    WalletV3R1 contract =
        WalletV3R1.builder().keyPair(keyPair).tonCenterClient(tonCenterClient).walletId(42).build();
    log.info("pub key: {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv key: {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonCenterClient, contract.getAddress(), Utils.toNano(1), true);
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract.getWalletId(),
        contract.getName(),
        Utils.formatNanoValue(balance));

    SendResponse response = contract.deploy();
    assertThat(response.getCode()).isZero();

    contract.waitForDeployment();

    // send toncoins
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .amount(Utils.toNano(0.8))
            .comment("ton4j testWalletV3R1")
            .build();

    response = contract.send(config);
    assertThat(response.getCode()).isZero();

    contract.waitForBalanceChangeWithTolerance(30, Utils.toNano(0.5));

    balance = contract.getBalance();
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract.getWalletId(),
        contract.getName(),
        Utils.formatNanoValue(balance));

    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.25).longValue());
  }
}
