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
import org.ton.ton4j.smartcontract.types.WalletV2R1Config;
import org.ton.ton4j.smartcontract.wallet.v2.WalletV2R1;
import org.ton.ton4j.toncenter.Network;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.TonResponse;
import org.ton.ton4j.toncenter.model.SendBocResponse;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.RawTransaction;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV2R1Short extends CommonTest {

  @Test
  public void testWalletV2R1() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV2R1 contract = WalletV2R1.builder().tonlib(tonlib).keyPair(keyPair).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment();

    // transfer coins from new wallet (back to faucet)
    WalletV2R1Config config =
        WalletV2R1Config.builder()
            .seqno(contract.getSeqno())
            .destination1(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount1(Utils.toNano(0.1))
            .build();

    RawTransaction rawTransaction = contract.sendWithConfirmation(config);
    assertThat(rawTransaction).isNotNull();

    log.info("sending to 4 destinations...");
    config =
        WalletV2R1Config.builder()
            .seqno(contract.getSeqno())
            .destination1(Address.of("EQA84DSUMyREa1Frp32wxFATnAVIXnWlYrbd3TFS1NLCbC-B"))
            .destination2(Address.of("EQCJZ3sJnes-o86xOa4LDDug6Lpz23RzyJ84CkTMIuVCCuan"))
            .destination3(Address.of("EQBjS7elE36MmEmE6-jbHQZNEEK0ObqRgaAxXWkx4pDGeefB"))
            .destination4(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"))
            .amount1(Utils.toNano(0.15))
            .amount2(Utils.toNano(0.15))
            .amount3(Utils.toNano(0.15))
            .amount4(Utils.toNano(0.15))
            .build();

    rawTransaction = contract.sendWithConfirmation(config);
    assertThat(rawTransaction).isNotNull();

    Utils.sleep(30);

    balance = contract.getBalance();
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.3).longValue());

    log.info("seqno {}", contract.getSeqno());
  }

  @Test
  public void testWalletSignedExternally() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();

    WalletV2R1 contract = WalletV2R1.builder().tonlib(tonlib).publicKey(publicKey).build();
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
    WalletV2R1Config config =
        WalletV2R1Config.builder()
            .seqno(1)
            .destination1(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount1(Utils.toNano(0.08))
            .comment("testWalletV2R1-signed-externally")
            .build();

    // transfer coins from new wallet (back to faucet) using externally signed body
    Cell transferBody = contract.createTransferBody(config);
    byte[] signedTransferBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash());
    sendResponse = contract.send(config, signedTransferBodyHash);
    log.info("extMessageInfo: {}", sendResponse);
    contract.waitForBalanceChange(120);
    Assertions.assertThat(contract.getBalance()).isLessThan(Utils.toNano(0.03));
  }

  @Test
  public void testWalletSignedExternallyLiteClient() throws Exception {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();
    WalletV2R1 contract =
        WalletV2R1.builder().adnlLiteClient(adnlLiteClient).publicKey(publicKey).build();
    log.info("pub key: {}", Utils.bytesToHex(publicKey));

    BigInteger balance =
        TestnetFaucet.topUpContract(adnlLiteClient, contract.getAddress(), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy using externally signed body
    Cell deployBody = contract.createDeployMessage();

    byte[] signedDeployBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), deployBody.hash());

    SendResponse sendResponse = contract.deploy(signedDeployBodyHash);
    log.info("extMessageInfo {}", sendResponse);
    contract.waitForDeployment(120);

    // send toncoins
    WalletV2R1Config config =
        WalletV2R1Config.builder()
            .seqno(1)
            .destination1(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount1(Utils.toNano(0.08))
            .comment("testWalletV2R1-signed-externally")
            .build();

    // transfer coins from new wallet (back to faucet) using externally signed body
    Cell transferBody = contract.createTransferBody(config);
    byte[] signedTransferBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash());
    sendResponse = contract.send(config, signedTransferBodyHash);
    log.info("extMessageInfo: {}", sendResponse);
    contract.waitForBalanceChange(120);
    Assertions.assertThat(contract.getBalance()).isLessThan(Utils.toNano(0.03));
  }

  @Test
  public void testWalletSignedExternallyTonCenterClient() throws Exception {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();

    TonCenter tonCenterClient =
        TonCenter.builder().apiKey(TESTNET_API_KEY).network(Network.TESTNET).build();

    WalletV2R1 contract =
        WalletV2R1.builder().publicKey(publicKey).tonCenterClient(tonCenterClient).build();
    log.info("pub key: {}", Utils.bytesToHex(publicKey));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonCenterClient, contract.getAddress(), Utils.toNano(1), true);
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy using externally signed body
    Cell deployBody = contract.createDeployMessage();

    byte[] signedDeployBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), deployBody.hash());

    // Deploy wallet
    TonResponse<SendBocResponse> response =
        tonCenterClient.sendBoc(
            contract.prepareDeployMsg(signedDeployBodyHash).toCell().toBase64());
    assertThat(response.isSuccess()).isTrue();

    Utils.sleep(20);

    // send toncoins
    WalletV2R1Config config =
        WalletV2R1Config.builder()
            .seqno(1)
            .destination1(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount1(Utils.toNano(0.8))
            .comment("ton4j testWalletV2R1-signed-externally")
            .build();

    // transfer coins from new wallet (back to faucet) using externally signed body
    Cell transferBody = contract.createTransferBody(config);
    byte[] signedTransferBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash());

    response =
        tonCenterClient.sendBoc(
            contract.prepareExternalMsg(config, signedTransferBodyHash).toCell().toBase64());
    assertThat(response.isSuccess()).isTrue();

    Utils.sleep(20);

    balance = contract.getBalance();
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    Assertions.assertThat(balance).isLessThan(Utils.toNano(0.3));
  }
}
