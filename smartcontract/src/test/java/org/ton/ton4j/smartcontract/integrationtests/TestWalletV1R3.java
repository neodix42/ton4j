package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.mnemonic.Mnemonic;
import org.ton.ton4j.mnemonic.Pair;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.WalletV1R3Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R3;
import org.ton.ton4j.tlb.Message;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.QueryFees;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R3 extends CommonTest {

  @Test
  public void testWalletV1R3() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV1R3 contract = WalletV1R3.builder().tonlib(tonlib).keyPair(keyPair).build();

    Cell deployMessage = CellBuilder.beginCell().storeUint(BigInteger.ZERO, 32).endCell();
    Message msg =
        MsgUtils.createExternalMessageWithSignedBody(
            keyPair, contract.getAddress(), contract.getStateInit(), deployMessage);
    Address address = msg.getInit().getAddress();

    String nonBounceableAddress = address.toNonBounceable();
    String bounceableAddress = address.toBounceable();

    String my = "Creating new wallet in workchain " + contract.getWc() + "\n";
    my = my + "Loading private key from file new-wallet.pk" + "\n";
    my = my + "StateInit: " + msg.getInit().toCell().print() + "\n";
    my = my + "new wallet address = " + address.toString(false) + "\n";
    my = my + "(Saving address to file new-wallet.addr)" + "\n";
    my = my + "Non-bounceable address (for init): " + nonBounceableAddress + "\n";
    my = my + "Bounceable address (for later access): " + bounceableAddress + "\n";
    my = my + "signing message: " + msg.getBody().print() + "\n";
    my = my + "External message for initialization is " + msg.toCell().print() + "\n";
    my = my + Utils.bytesToHex(msg.toCell().toBoc()).toUpperCase() + "\n";
    my = my + "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
    log.info(my);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy new wallet
    ExtMessageInfo extMessageInfo = tonlib.sendRawMessage(msg.toCell().toBase64());
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(20);

    WalletV1R3Config config =
        WalletV1R3Config.builder()
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testNewWalletV1R3")
            .build();

    // transfer coins from new wallet (back to faucet)
    SendResponse sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForBalanceChange(30);

    balance = new BigInteger(tonlib.getRawAccountState(address).getBalance());
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());

    log.info("seqno {}", contract.getSeqno());
    log.info("pubkey {}", contract.getPublicKey());
  }

  @Test
  public void testWalletV1R3WithMnemonic()
      throws InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
    List<String> mnemonic = Mnemonic.generate(24);
    Pair keyPair = Mnemonic.toKeyPair(mnemonic);

    log.info("pubkey {}", Utils.bytesToHex(keyPair.getPublicKey()));
    log.info("seckey {}", Utils.bytesToHex(keyPair.getSecretKey()));

    TweetNaclFast.Signature.KeyPair keyPairSig =
        TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

    log.info("pubkey {}", Utils.bytesToHex(keyPairSig.getPublicKey()));
    log.info("seckey {}", Utils.bytesToHex(keyPairSig.getSecretKey()));

    WalletV1R3 contract = WalletV1R3.builder().tonlib(tonlib).keyPair(keyPairSig).build();

    Message msg =
        MsgUtils.createExternalMessageWithSignedBody(
            keyPairSig,
            contract.getAddress(),
            contract.getStateInit(),
            CellBuilder.beginCell().storeUint(BigInteger.ZERO, 32).endCell());
    Address address = msg.getInit().getAddress();

    String nonBounceableAddress = address.toBounceable();
    String bounceableAddress = address.toNonBounceable();

    String my = "Creating new wallet in workchain " + contract.getWc() + "\n";
    my = my + "Loading private key from file new-wallet.pk" + "\n";
    my = my + "StateInit: " + msg.getInit().toCell().print() + "\n";
    my = my + "new wallet address = " + address.toString(false) + "\n";
    my = my + "(Saving address to file new-wallet.addr)" + "\n";
    my = my + "Non-bounceable address (for init): " + nonBounceableAddress + "\n";
    my = my + "Bounceable address (for later access): " + bounceableAddress + "\n";
    my = my + "signing message: " + msg.getBody().print() + "\n";
    my = my + "External message for initialization is " + msg.toCell().print() + "\n";
    my = my + Utils.bytesToHex(msg.toCell().toBoc()).toUpperCase() + "\n";
    my = my + "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
    log.info(my);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy new wallet
    ExtMessageInfo extMessageInfo = tonlib.sendRawMessage(msg.toCell().toBase64());
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(30);

    WalletV1R3Config config =
        WalletV1R3Config.builder()
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testNewWalletV1R3")
            .build();

    // transfer coins from new wallet (back to faucet)
    SendResponse sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForBalanceChange(30);

    balance = new BigInteger(tonlib.getRawAccountState(address).getBalance());
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());

    log.info("seqno {}", contract.getSeqno());
    log.info("pubkey {}", contract.getPublicKey());
  }

  @Test
  public void testWalletV1R3EstimateFees() throws NoSuchAlgorithmException, InvalidKeyException {
    List<String> mnemonic = Mnemonic.generate(24);
    Pair keyPair = Mnemonic.toKeyPair(mnemonic);

    log.info("pubkey {}", Utils.bytesToHex(keyPair.getPublicKey()));
    log.info("seckey {}", Utils.bytesToHex(keyPair.getSecretKey()));

    TweetNaclFast.Signature.KeyPair keyPairSig =
        TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

    log.info("pubkey {}", Utils.bytesToHex(keyPairSig.getPublicKey()));
    log.info("seckey {}", Utils.bytesToHex(keyPairSig.getSecretKey()));

    WalletV1R3 contract = WalletV1R3.builder().keyPair(keyPairSig).build();

    Message msg =
        MsgUtils.createExternalMessageWithSignedBody(
            keyPairSig, contract.getAddress(), contract.getStateInit(), null);

    QueryFees feesWithCodeData =
        tonlib.estimateFees(
            msg.getInit().getAddress().toString(),
            msg.getBody().toBase64(), // message to cell not the whole external
            msg.getInit().getCode().toBase64(),
            msg.getInit().getData().toBase64(),
            false);

    log.info("fees {}", feesWithCodeData);
    assertThat(feesWithCodeData).isNotNull();

    QueryFees feesBodyOnly =
        tonlib.estimateFees(
            msg.getInit().getAddress().toString(), msg.getBody().toBase64(), null, null, false);
    log.info("fees {}", feesBodyOnly);
    assertThat(feesBodyOnly).isNotNull();
  }

  @Test
  public void testWalletSignedExternally() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();

    WalletV1R3 contract = WalletV1R3.builder().tonlib(tonlib).publicKey(publicKey).build();
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
    WalletV1R3Config config =
        WalletV1R3Config.builder()
            .seqno(1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testWalletV1R3-signed-externally")
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
  public void testWalletSignedExternallyAdnlClient() throws Exception {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();
    WalletV1R3 contract =
        WalletV1R3.builder().adnlLiteClient(adnlLiteClient).publicKey(publicKey).build();
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
    WalletV1R3Config config =
        WalletV1R3Config.builder()
            .seqno(1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testWalletV1R3-signed-externally")
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
        TonCenter.builder().apiKey(TESTNET_API_KEY).testnet().debug().build();
    WalletV1R3 contract =
        WalletV1R3.builder().tonCenterClient(tonCenterClient).publicKey(publicKey).build();
    log.info("pub key: {}", Utils.bytesToHex(publicKey));
    log.info("address: {}", contract.getAddress().toBounceable());

    BigInteger balance =
        TestnetFaucet.topUpContract(
            tonCenterClient, contract.getAddress(), Utils.toNano(1), true);
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    Utils.sleep(2);
    log.info(
        "new wallet {} balance: {}",
        contract.getName(),
        Utils.formatNanoValue(contract.getBalance()));

    // deploy using externally signed body
    Cell deployBody = contract.createDeployMessage();

    byte[] signedDeployBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), deployBody.hash());

    SendResponse sendResponse = contract.deploy(signedDeployBodyHash);
    log.info("sendResponse {}", sendResponse);
    contract.waitForDeployment();

    // send toncoins
    WalletV1R3Config config =
        WalletV1R3Config.builder()
            .seqno(1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.8))
            .comment("ton4j testWalletV1R3-signed-externally")
            .build();

    // transfer coins from new wallet (back to faucet) using externally signed body
    Cell transferBody = contract.createTransferBody(config);
    byte[] signedTransferBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash());
    sendResponse = contract.send(config, signedTransferBodyHash);
    log.info("sendResponse: {}", sendResponse);
    Utils.sleep(2);
    contract.waitForBalanceChange();
    Utils.sleep(2);
    balance = contract.getBalance();
    log.info("wallet {} new balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    Assertions.assertThat(balance).isLessThan(Utils.toNano(0.3));
  }
}
