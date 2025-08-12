package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.DeployedPlugin;
import org.ton.ton4j.smartcontract.types.NewPlugin;
import org.ton.ton4j.smartcontract.types.WalletV4R2Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.v4.SubscriptionInfo;
import org.ton.ton4j.smartcontract.wallet.v4.WalletV4R2;
import org.ton.ton4j.tlb.Message;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV4R2Plugins extends CommonTest {

  static String FAUCET_ADDRESS_RAW =
      "0:b52a16ba3735501df19997550e7ed4c41754ee501ded8a841088ce4278b66de4";

  @Test
  public void testWalletV4Deploy() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV4R2 contract = WalletV4R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();
  }

  @Test
  public void testPlugins() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV4R2 contract = WalletV4R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(7));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    log.info("contract balance {}", Utils.formatNanoValue(contract.getBalance()));
    // deploy wallet-v4
    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(30);

    long walletCurrentSeqno = contract.getSeqno();
    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));
    log.info("seqno: {}", walletCurrentSeqno);
    log.info("walletId: {}", contract.getWalletId());
    log.info("pubKey: {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("pluginsList: {}", contract.getPluginsList());
    assertThat(contract.getPluginsList().isEmpty()).isTrue();

    // create and deploy plugin -- start

    Address beneficiaryAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");
    log.info("beneficiaryAddress: {}", beneficiaryAddress.toBounceable());

    SubscriptionInfo subscriptionInfo =
        SubscriptionInfo.builder()
            .beneficiary(beneficiaryAddress)
            .subscriptionFee(Utils.toNano(2))
            .period(60)
            .startTime(0)
            .timeOut(30)
            .lastPaymentTime(0)
            .lastRequestTime(0)
            .failedAttempts(0)
            .subscriptionId(12345)
            .build();

    Utils.sleep(30);

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(tonlib.getAccountBalance(beneficiaryAddress)));

    WalletV4R2Config config =
        WalletV4R2Config.builder()
            .seqno(contract.getSeqno())
            .operation(1) // deploy and install plugin
            .walletId(42)
            .newPlugin(
                NewPlugin.builder()
                    .secretKey(keyPair.getSecretKey())
                    .seqno(walletCurrentSeqno)
                    .pluginWc(contract.getWc()) // reuse wc of the wallet
                    .amount(
                        Utils.toNano(0.1)) // initial plugin balance, will be taken from wallet-v4
                    .stateInit(contract.createPluginStateInit(subscriptionInfo))
                    .body(contract.createPluginBody())
                    .build())
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(45);

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(tonlib.getAccountBalance(beneficiaryAddress)));

    // create and deploy plugin -- end

    // get plugin list
    List<String> plugins = contract.getPluginsList();
    log.info("pluginsList: {}", plugins);

    assertThat(plugins.isEmpty()).isFalse();

    Address pluginAddress = Address.of(plugins.get(0));
    log.info("pluginAddress {}", pluginAddress.toString(false));

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);

    log.info("{}", subscriptionInfo);
    log.info(
        "plugin hash: int {}, hex {}",
        new BigInteger(pluginAddress.hashPart),
        Utils.bytesToHex(pluginAddress.hashPart));

    log.info("plugin {} installed {}", pluginAddress, contract.isPluginInstalled(pluginAddress));

    log.info("collect fee - first time");

    Cell extMessage =
        MsgUtils.createExternalMessageWithSignedBody(
                contract.getKeyPair(), pluginAddress, null, null)
            .toCell();

    ExtMessageInfo extMessageInfo = tonlib.sendRawMessage(extMessage.toBase64());
    log.info("extMessageInfo {}", extMessageInfo);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    tonlib.waitForDeployment(beneficiaryAddress, 90);
    //    ContractUtils.waitForDeployment(tonlib, beneficiaryAddress, 90); // no need?

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(tonlib.getAccountBalance(beneficiaryAddress)));

    Utils.sleep(30, "wait for seqno update");

    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);
    log.info("{}", subscriptionInfo);

    assertThat(subscriptionInfo.getLastPaymentTime()).isNotEqualTo(0);

    log.info("collect fee - second time");

    Utils.sleep(180, "wait for timeout");

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);
    log.info("{}", subscriptionInfo);

    extMessage =
        MsgUtils.createExternalMessageWithSignedBody(
                contract.getKeyPair(), pluginAddress, null, null)
            .toCell();
    extMessageInfo = tonlib.sendRawMessage(extMessage.toBase64());
    log.info("extMessageInfo {}", extMessageInfo);
    assertThat(sendResponse.getCode()).isZero();

    tonlib.waitForDeployment(subscriptionInfo.getBeneficiary(), 90);
    Utils.sleep(10);

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(tonlib.getAccountBalance(subscriptionInfo.getBeneficiary())));

    Utils.sleep(30);

    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);
    log.info("{}", subscriptionInfo);

    // uninstall/remove plugin from the wallet -- start

    log.info("Uninstalling plugin {}", Address.of(contract.getPluginsList().get(0)));

    walletCurrentSeqno = contract.getSeqno();

    config =
        WalletV4R2Config.builder()
            .seqno(contract.getSeqno())
            .walletId(config.getWalletId())
            .operation(3) // uninstall plugin
            .deployedPlugin(
                DeployedPlugin.builder()
                    .seqno(walletCurrentSeqno)
                    .amount(Utils.toNano(0.1))
                    .pluginAddress(Address.of(contract.getPluginsList().get(0)))
                    .secretKey(keyPair.getSecretKey())
                    .queryId(0)
                    .build())
            .build();

    sendResponse = contract.uninstallPlugin(config);
    Utils.sleep(30, "sent uninstall request");
    assertThat(sendResponse.getCode()).isZero();

    // uninstall plugin -- end

    Utils.sleep(60);
    List<String> list = contract.getPluginsList();
    log.info("pluginsList: {}", list);
    assertThat(list.isEmpty()).isTrue();

    config =
        WalletV4R2Config.builder()
            .operation(0)
            .walletId(contract.getWalletId())
            .seqno(contract.getSeqno())
            .destination(Address.of(FAUCET_ADDRESS_RAW))
            .amount(Utils.toNano(0.331))
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();
  }

  @Test
  public void testSimpleSend() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV4R2 contract = WalletV4R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v4
    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(30);

    long walletCurrentSeqno = contract.getSeqno();
    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));
    log.info("seqno: {}", walletCurrentSeqno);
    log.info("walletId: {}", contract.getWalletId());
    log.info("pubKey: {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("pluginsList: {}", contract.getPluginsList());

    WalletV4R2Config config =
        WalletV4R2Config.builder()
            .operation(0)
            .walletId(contract.getWalletId())
            .seqno(contract.getSeqno())
            .destination(Address.of(FAUCET_ADDRESS_RAW))
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .amount(Utils.toNano(0.331))
            .comment("ton4j-v4r2-simple-send")
            .build();

    contract.send(config);
  }

  @Test
  public void testSimpleSend_ExternallySigned() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] pubKey = keyPair.getPublicKey();
    WalletV4R2 contract =
        WalletV4R2.builder().tonlib(tonlib).publicKey(pubKey).walletId(42).build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(pubKey));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v4
    Cell deployBody = contract.createDeployMessage();
    byte[] signedDeployBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), deployBody.hash());

    SendResponse sendResponse = contract.deploy(signedDeployBodyHash);
    assertThat(sendResponse.getCode()).isZero();
    contract.waitForDeployment(30);

    long walletCurrentSeqno = contract.getSeqno();
    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));
    log.info("seqno: {}", walletCurrentSeqno);
    log.info("walletId: {}", contract.getWalletId());
    log.info("pubKey: {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("pluginsList: {}", contract.getPluginsList());

    WalletV4R2Config config =
        WalletV4R2Config.builder()
            .operation(0)
            .walletId(contract.getWalletId())
            .seqno(contract.getSeqno())
            .destination(Address.of(FAUCET_ADDRESS_RAW))
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .amount(Utils.toNano(0.331))
            .comment("ton4j-v4r2-simple-send-externally-signed")
            .build();
    // transfer coins from new wallet (back to faucet) using externally signed body
    Cell transferBody = contract.createTransferBody(config);
    byte[] signedTransferBodyHash =
        Utils.signData(pubKey, keyPair.getSecretKey(), transferBody.hash());
    sendResponse = contract.send(config, signedTransferBodyHash);
    log.info("extMessageInfo: {}", sendResponse);
    contract.waitForBalanceChange();
    assertThat(contract.getBalance()).isLessThan(Utils.toNano(0.7));
  }

  @Test
  public void testPluginsAdnlLiteClient() throws Exception {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();

    WalletV4R2 contract =
        WalletV4R2.builder().adnlLiteClient(adnlLiteClient).keyPair(keyPair).walletId(42).build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(nonBounceableAddress), Utils.toNano(7));
    log.info("wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v4
    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment(30);

    long walletCurrentSeqno = contract.getSeqno();
    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));
    log.info("seqno: {}", walletCurrentSeqno);
    log.info("walletId: {}", contract.getWalletId());
    log.info("pubKey: {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("pluginsList: {}", contract.getPluginsList());
    assertThat(contract.getPluginsList().isEmpty()).isTrue();

    // create and deploy plugin -- start

    Address beneficiaryAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");
    log.info("beneficiaryAddress: {}", beneficiaryAddress.toBounceable());

    SubscriptionInfo subscriptionInfo =
        SubscriptionInfo.builder()
            .beneficiary(beneficiaryAddress)
            .subscriptionFee(Utils.toNano(2))
            .period(60)
            .startTime(0)
            .timeOut(30)
            .lastPaymentTime(0)
            .lastRequestTime(0)
            .failedAttempts(0)
            .subscriptionId(12345)
            .build();

    Utils.sleep(30);

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(adnlLiteClient.getBalance(beneficiaryAddress)));

    WalletV4R2Config config =
        WalletV4R2Config.builder()
            .seqno(contract.getSeqno())
            .operation(1) // deploy and install plugin
            .walletId(42)
            .newPlugin(
                NewPlugin.builder()
                    .secretKey(keyPair.getSecretKey())
                    .seqno(walletCurrentSeqno)
                    .pluginWc(contract.getWc()) // reuse wc of the wallet
                    .amount(
                        Utils.toNano(0.1)) // initial plugin balance, will be taken from wallet-v4
                    .stateInit(contract.createPluginStateInit(subscriptionInfo))
                    .body(contract.createPluginBody())
                    .build())
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(45);

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(adnlLiteClient.getBalance(beneficiaryAddress)));

    // create and deploy plugin -- end

    // get plugin list
    List<String> plugins = contract.getPluginsList();
    log.info("pluginsList: {}", plugins);

    assertThat(plugins.isEmpty()).isFalse();

    Address pluginAddress = Address.of(plugins.get(0));
    log.info("pluginAddress {}", pluginAddress.toString(false));

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);

    log.info("{}", subscriptionInfo);
    log.info(
        "plugin hash: int {}, hex {}",
        new BigInteger(pluginAddress.hashPart),
        Utils.bytesToHex(pluginAddress.hashPart));

    log.info("plugin {} installed {}", pluginAddress, contract.isPluginInstalled(pluginAddress));

    log.info("collect fee - first time");

    Message extMessage =
        MsgUtils.createExternalMessageWithSignedBody(
            contract.getKeyPair(), pluginAddress, null, null);

    adnlLiteClient.sendMessage(extMessage);
    log.info("extMessageInfo {}", sendResponse);
    assertThat(sendResponse.getCode()).isZero();

    adnlLiteClient.waitForDeployment(beneficiaryAddress, 90);
    //    ContractUtils.waitForDeployment(tonlib, beneficiaryAddress, 90); // no need?

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(adnlLiteClient.getBalance(beneficiaryAddress)));

    Utils.sleep(30, "wait for seqno update");

    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);
    log.info("{}", subscriptionInfo);

    assertThat(subscriptionInfo.getLastPaymentTime()).isNotEqualTo(0);

    log.info("collect fee - second time");

    Utils.sleep(180, "wait for timeout");

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);
    log.info("{}", subscriptionInfo);

    extMessage =
        MsgUtils.createExternalMessageWithSignedBody(
            contract.getKeyPair(), pluginAddress, null, null);
    adnlLiteClient.sendMessage(extMessage);

    adnlLiteClient.waitForDeployment(subscriptionInfo.getBeneficiary(), 90);

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(adnlLiteClient.getBalance(subscriptionInfo.getBeneficiary())));

    Utils.sleep(30);

    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);
    log.info("{}", subscriptionInfo);

    // uninstall/remove plugin from the wallet -- start

    log.info("Uninstalling plugin {}", Address.of(contract.getPluginsList().get(0)));

    walletCurrentSeqno = contract.getSeqno();

    config =
        WalletV4R2Config.builder()
            .seqno(contract.getSeqno())
            .walletId(config.getWalletId())
            .operation(3) // uninstall plugin
            .deployedPlugin(
                DeployedPlugin.builder()
                    .seqno(walletCurrentSeqno)
                    .amount(Utils.toNano(0.1))
                    .pluginAddress(Address.of(contract.getPluginsList().get(0)))
                    .secretKey(keyPair.getSecretKey())
                    .queryId(0)
                    .build())
            .build();

    sendResponse = contract.uninstallPlugin(config);
    Utils.sleep(30, "sent uninstall request");
    assertThat(sendResponse.getCode()).isZero();

    // uninstall plugin -- end

    Utils.sleep(60);
    List<String> list = contract.getPluginsList();
    log.info("pluginsList: {}", list);
    assertThat(list.isEmpty()).isTrue();

    config =
        WalletV4R2Config.builder()
            .operation(0)
            .walletId(contract.getWalletId())
            .seqno(contract.getSeqno())
            .destination(Address.of(FAUCET_ADDRESS_RAW))
            .amount(Utils.toNano(0.331))
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();
  }

  @Test
  public void testPluginsTonCenterClient() throws Exception {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    TonCenter tonCenter = TonCenter.builder().apiKey("your_api_key").testnet().build();

    WalletV4R2 contract =
        WalletV4R2.builder().tonCenterClient(tonCenter).keyPair(keyPair).walletId(42).build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(
            tonCenter, Address.of(nonBounceableAddress), Utils.toNano(7), true);
    log.info("wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy wallet-v4
    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForDeployment();

    long walletCurrentSeqno = contract.getSeqno();
    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));
    log.info("seqno: {}", walletCurrentSeqno);
    Utils.sleep(2);
    log.info("walletId: {}", contract.getWalletId());
    Utils.sleep(2);
    log.info("pubKey: {}", Utils.bytesToHex(contract.getPublicKey()));
    Utils.sleep(2);
    log.info("pluginsList: {}", contract.getPluginsList());
    Utils.sleep(2);
    assertThat(contract.getPluginsList().isEmpty()).isTrue();

    // create and deploy plugin -- start

    Address beneficiaryAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");
    log.info("beneficiaryAddress: {}", beneficiaryAddress.toBounceable());

    SubscriptionInfo subscriptionInfo =
        SubscriptionInfo.builder()
            .beneficiary(beneficiaryAddress)
            .subscriptionFee(Utils.toNano(2))
            .period(60)
            .startTime(0)
            .timeOut(30)
            .lastPaymentTime(0)
            .lastRequestTime(0)
            .failedAttempts(0)
            .subscriptionId(12345)
            .build();

    Utils.sleep(20);

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(tonCenter.getBalance(beneficiaryAddress.toBounceable())));

    long seqno = contract.getSeqno();
    Utils.sleep(2);
    long wc = contract.getWc();
    Utils.sleep(2);

    WalletV4R2Config config =
        WalletV4R2Config.builder()
            .seqno(seqno)
            .operation(1) // deploy and install plugin
            .walletId(42)
            .newPlugin(
                NewPlugin.builder()
                    .secretKey(keyPair.getSecretKey())
                    .seqno(walletCurrentSeqno)
                    .pluginWc(wc) // reuse wc of the wallet
                    .amount(
                        Utils.toNano(0.1)) // initial plugin balance, will be taken from wallet-v4
                    .stateInit(contract.createPluginStateInit(subscriptionInfo))
                    .body(contract.createPluginBody())
                    .build())
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(20);

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(tonCenter.getBalance(beneficiaryAddress.toBounceable())));

    // create and deploy plugin -- end
    Utils.sleep(2);
    // get plugin list
    List<String> plugins = contract.getPluginsList();
    log.info("pluginsList: {}", plugins);

    assertThat(plugins.isEmpty()).isFalse();

    Address pluginAddress = Address.of(plugins.get(0));
    log.info("pluginAddress {}", pluginAddress.toString(false));

    Utils.sleep(2);
    subscriptionInfo = contract.getSubscriptionData(pluginAddress);

    log.info("{}", subscriptionInfo);
    log.info(
        "plugin hash: int {}, hex {}",
        new BigInteger(pluginAddress.hashPart),
        Utils.bytesToHex(pluginAddress.hashPart));

    Utils.sleep(2);
    log.info("plugin {} installed {}", pluginAddress, contract.isPluginInstalled(pluginAddress));

    log.info("collect fee - first time");

    Message extMessage =
        MsgUtils.createExternalMessageWithSignedBody(
            contract.getKeyPair(), pluginAddress, null, null);

    tonCenter.sendBoc(extMessage.toCell().toBase64());
    log.info("extMessageInfo {}", sendResponse);
    assertThat(sendResponse.getCode()).isZero();

    // Wait for deployment - custom implementation since TonCenter doesn't have waitForDeployment
    int timeoutSeconds = 90;
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Can't deploy contract within specified timeout.");
      }
      Utils.sleep(2);
    } while (!"active".equals(tonCenter.getState(beneficiaryAddress.toBounceable())));
    //    ContractUtils.waitForDeployment(tonlib, beneficiaryAddress, 90); // no need?

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(tonCenter.getBalance(beneficiaryAddress.toBounceable())));

    Utils.sleep(30, "wait for seqno update");

    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);
    log.info("{}", subscriptionInfo);

    assertThat(subscriptionInfo.getLastPaymentTime()).isNotEqualTo(0);

    log.info("collect fee - second time");

    Utils.sleep(180, "wait for timeout");

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);
    log.info("{}", subscriptionInfo);

    extMessage =
        MsgUtils.createExternalMessageWithSignedBody(
            contract.getKeyPair(), pluginAddress, null, null);
    tonCenter.sendBoc(extMessage.toCell().toBase64());

    // Wait for deployment - custom implementation since TonCenter doesn't have waitForDeployment
    timeoutSeconds = 90;
    i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Can't deploy contract within specified timeout.");
      }
      Utils.sleep(2);
    } while (!"active"
        .equals(tonCenter.getState(subscriptionInfo.getBeneficiary().toBounceable())));

    log.info(
        "beneficiaryWallet balance {}",
        Utils.formatNanoValue(
            tonCenter.getBalance(subscriptionInfo.getBeneficiary().toBounceable())));

    Utils.sleep(30);

    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);
    log.info("{}", subscriptionInfo);

    // uninstall/remove plugin from the wallet -- start

    log.info("Uninstalling plugin {}", Address.of(contract.getPluginsList().get(0)));

    walletCurrentSeqno = contract.getSeqno();

    config =
        WalletV4R2Config.builder()
            .seqno(contract.getSeqno())
            .walletId(config.getWalletId())
            .operation(3) // uninstall plugin
            .deployedPlugin(
                DeployedPlugin.builder()
                    .seqno(walletCurrentSeqno)
                    .amount(Utils.toNano(0.1))
                    .pluginAddress(Address.of(contract.getPluginsList().get(0)))
                    .secretKey(keyPair.getSecretKey())
                    .queryId(0)
                    .build())
            .build();

    sendResponse = contract.uninstallPlugin(config);
    Utils.sleep(30, "sent uninstall request");
    assertThat(sendResponse.getCode()).isZero();

    // uninstall plugin -- end

    Utils.sleep(60);
    List<String> list = contract.getPluginsList();
    log.info("pluginsList: {}", list);
    assertThat(list.isEmpty()).isTrue();

    config =
        WalletV4R2Config.builder()
            .operation(0)
            .walletId(contract.getWalletId())
            .seqno(contract.getSeqno())
            .destination(Address.of(FAUCET_ADDRESS_RAW))
            .amount(Utils.toNano(0.331))
            .build();

    sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();
  }
}
