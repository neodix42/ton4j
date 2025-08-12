package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R1;
import org.ton.ton4j.tlb.Message;
import org.ton.ton4j.toncenter.Network;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.TonResponse;
import org.ton.ton4j.toncenter.model.SendBocResponse;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.RawTransaction;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV3R1 extends CommonTest {
  @Test
  public void testWalletV3R1() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV3R1 contract = WalletV3R1.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    Message msg =
        MsgUtils.createExternalMessageWithSignedBody(
            keyPair,
            contract.getAddress(),
            contract.getStateInit(),
            CellBuilder.beginCell()
                .storeUint(42, 32) // subwallet
                .storeUint(Instant.now().getEpochSecond() + 5 * 60L, 32) // valid-until
                .storeUint(0, 32) // seqno
                .endCell());
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
    my = my + Utils.bytesToHex(msg.getBody().toBoc()).toUpperCase() + "\n";
    my = my + "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
    log.info(my);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy new wallet
    ExtMessageInfo extMessageInfo = tonlib.sendRawMessage(msg.toCell().toBase64());
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(40);

    // try to transfer coins from new wallet (back to faucet)
    WalletV3Config config =
        WalletV3Config.builder()
            .seqno(contract.getSeqno())
            .walletId(42)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.8))
            .comment("testWalletV3R1")
            .build();

    SendResponse sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForBalanceChange(90);

    balance = contract.getBalance();
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.2).longValue());
  }

  @Test
  public void testWalletV3R1SendRawMessageWithConfirmation() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV3R1 contract = WalletV3R1.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    Message msg =
        MsgUtils.createExternalMessageWithSignedBody(
            keyPair,
            contract.getAddress(),
            contract.getStateInit(),
            CellBuilder.beginCell()
                .storeUint(42, 32) // subwallet
                .storeUint(Instant.now().getEpochSecond() + 5 * 60L, 32) // valid-until
                .storeUint(0, 32) // seqno
                .endCell());
    Address address = msg.getInit().getAddress();

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(address.toNonBounceable()), Utils.toNano(1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy new wallet
    RawTransaction tx = tonlib.sendRawMessageWithConfirmation(msg.toCell().toBase64(), address);
    log.info("msg found in tx {}", tx);
    assertThat(tx).isNotNull();

    contract.waitForDeployment(40);

    // try to transfer coins from new wallet (back to faucet)
    WalletV3Config config =
        WalletV3Config.builder()
            .seqno(contract.getSeqno())
            .walletId(42)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.8))
            .comment("testWalletV3R1")
            .build();

    SendResponse sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForBalanceChange(90);

    balance = contract.getBalance();
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.2).longValue());
  }

  @Test
  public void testWalletV3R1SendRawMessageWithConfirmationAdnlLiteClient() throws Exception {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();
    WalletV3R1 contract =
        WalletV3R1.builder().adnlLiteClient(adnlLiteClient).keyPair(keyPair).walletId(42).build();

    Message msg =
        MsgUtils.createExternalMessageWithSignedBody(
            keyPair,
            contract.getAddress(),
            contract.getStateInit(),
            CellBuilder.beginCell()
                .storeUint(42, 32) // subwallet
                .storeUint(Instant.now().getEpochSecond() + 5 * 60L, 32) // valid-until
                .storeUint(0, 32) // seqno
                .endCell());
    Address address = msg.getInit().getAddress();

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(address.toNonBounceable()), Utils.toNano(1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy new wallet
    adnlLiteClient.sendRawMessageWithConfirmation(msg, address);

    contract.waitForDeployment(40);

    // try to transfer coins from new wallet (back to faucet)
    WalletV3Config config =
        WalletV3Config.builder()
            .seqno(contract.getSeqno())
            .walletId(42)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.8))
            .comment("testWalletV3R1")
            .build();

    SendResponse sendResponse = contract.send(config);
    assertThat(sendResponse.getCode()).isZero();

    contract.waitForBalanceChange(90);

    balance = contract.getBalance();
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.2).longValue());
  }

  @Test
  public void testWalletV3R1SendRawMessageWithConfirmationTonCenterClient() throws Exception {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    TonCenter tonCenterClient =
        TonCenter.builder().apiKey(TESTNET_API_KEY).network(Network.TESTNET).build();

    WalletV3R1 contract =
        WalletV3R1.builder().keyPair(keyPair).tonCenterClient(tonCenterClient).walletId(42).build();

    Message msg =
        MsgUtils.createExternalMessageWithSignedBody(
            keyPair,
            contract.getAddress(),
            contract.getStateInit(),
            CellBuilder.beginCell()
                .storeUint(42, 32) // subwallet
                .storeUint(Instant.now().getEpochSecond() + 5 * 60L, 32) // valid-until
                .storeUint(0, 32) // seqno
                .endCell());
    Address address = msg.getInit().getAddress();

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            tonCenterClient, Address.of(address.toNonBounceable()), Utils.toNano(1), true);
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy new wallet
    TonResponse<SendBocResponse> response = tonCenterClient.sendBoc(msg.toCell().toBase64());
    assertThat(response.isSuccess()).isTrue();

    Utils.sleep(20);

    // try to transfer coins from new wallet (back to faucet)
    WalletV3Config config =
        WalletV3Config.builder()
            .seqno(1)
            .walletId(42)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.8))
            .comment("testWalletV3R1")
            .build();

    response = tonCenterClient.sendBoc(contract.prepareExternalMsg(config).toCell().toBase64());
    assertThat(response.isSuccess()).isTrue();

    Utils.sleep(20);

    balance = contract.getBalance();
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.2).longValue());
  }
}
