package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ton.ton4j.smartcontract.integrationtests.CommonTest.TESTNET_API_KEY;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.GenerateWallet;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.token.ft.JettonMinterStableCoin;
import org.ton.ton4j.smartcontract.token.ft.JettonWalletStableCoin;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.JettonMinterData;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R1;
import org.ton.ton4j.toncenter.Network;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestJettonStableCoin {
  public static final String NEW_ADMIN2 = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";

  static WalletV3R1 adminWallet;
  static WalletV3R1 wallet2;
  static Tonlib tonlib;

  @Test
  public void testJettonMinterStableCoin() throws InterruptedException {
    tonlib =
        Tonlib.builder().testnet(true).pathToTonlibSharedLib(Utils.getTonlibGithubUrl()).build();
    adminWallet = GenerateWallet.randomV3R1(tonlib, 3);
    wallet2 = GenerateWallet.randomV3R1(tonlib, 2);

    JettonMinterStableCoin minter =
        JettonMinterStableCoin.builder()
            .tonlib(tonlib)
            .adminAddress(adminWallet.getAddress())
            .nextAdminAddress(Address.of(NEW_ADMIN2))
            .content(
                NftUtils.createOffChainUriCell(
                    "https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
            .jettonWalletCodeHex(WalletCodes.jettonWalletStableCoin.getValue())
            .build();

    log.info("jetton minter address {}", minter.getAddress());

    // DEPLOY MINTER AND MINT JETTONS

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(1.2))
            .stateInit(minter.getStateInit())
            .body(
                JettonMinterStableCoin.createMintBody(
                    0,
                    adminWallet.getAddress(),
                    Utils.toNano(0.1), // ton amount
                    Utils.toNano(100500), // jetton amount
                    null, // from address
                    null, // response address
                    BigInteger.ONE, // fwd amount
                    MsgUtils.createTextMessageBody("minting")) // forward payload
                )
            .build();

    SendResponse sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(60, "minting...");

    getMinterInfoV2(minter);

    assertThat(minter.getJettonData().getTotalSupply().longValue()).isNotEqualTo(0);

    // TRANSFER from adminWallet to wallet2 (by sending transfer request to admin's jettonWallet)
    JettonWalletStableCoin adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    Utils.toNano(200),
                    wallet2.getAddress(), // recipient
                    null, // response address
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody("gift") // forward payload
                    ))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring 200 jettons from adminWallet to wallet2...");

    getMinterInfoV2(minter);

    // TRANSFER from wallet2 to adminWallet (by sending transfer request to wallet2's jettonWallet)
    JettonWalletStableCoin jettonWallet2 = minter.getJettonWallet(wallet2.getAddress());

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());
    log.info(
        "    jettonWallet2 balance: {}, address: {}",
        Utils.formatNanoValue(jettonWallet2.getBalance(), 6),
        jettonWallet2.getAddress());

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(wallet2.getSeqno())
            .destination(jettonWallet2.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    Utils.toNano(100),
                    adminWallet.getAddress(), // recipient
                    null, // response address
                    Utils.toNano("0.01"), // forward amount
                    MsgUtils.createTextMessageBody("gift from wallet2") // forward payload
                    ))
            .build();

    sendResponse = wallet2.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring 100 jettons from wallet2 to adminWallet...");

    getMinterInfoV2(minter);

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());
    log.info(
        "    jettonWallet2 balance: {}, address: {}",
        Utils.formatNanoValue(jettonWallet2.getBalance(), 6),
        jettonWallet2.getAddress());

    // LOCK JETTONS via minter contract in walletJetton2

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.07))
            .body(
                JettonWalletStableCoin.createCallToBody(
                    0,
                    wallet2.getAddress(),
                    Utils.toNano(0.05),
                    JettonWalletStableCoin.createStatusBody(0, 3) // cant send and receive
                    ))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "locking jettons in jettonWallet2");

    // since the jettonWallet2 is locked the transfer and receive should FAIL
    // TRANSFER from wallet2 to adminWallet (by sending transfer request to wallet2's jettonWallet)
    jettonWallet2 = minter.getJettonWallet(wallet2.getAddress());

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(wallet2.getSeqno())
            .destination(jettonWallet2.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    Utils.toNano(50),
                    adminWallet.getAddress(), // recipient
                    null, // response address
                    Utils.toNano("0.01"), // forward amount
                    MsgUtils.createTextMessageBody("gift from wallet2")) // forward payload
                )
            .build();

    sendResponse = wallet2.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring 50 jettons after lock from wallet2 to adminWallet...");

    getMinterInfoV2(minter);

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());
    log.info(
        "    jettonWallet2 balance: {}, address: {}",
        Utils.formatNanoValue(jettonWallet2.getBalance(), 6),
        jettonWallet2.getAddress());

    // in explorer there will be an error Failed Compute Phase (exit_code 45)
    assertThat(jettonWallet2.getBalance()).isEqualTo(Utils.toNano(100));
  }

  @Test
  public void testJettonMinterStableCoinAdnlLiteClient() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();

    adminWallet = GenerateWallet.randomV3R1(adnlLiteClient, 3);
    wallet2 = GenerateWallet.randomV3R1(adnlLiteClient, 2);

    JettonMinterStableCoin minter =
        JettonMinterStableCoin.builder()
            .adnlLiteClient(adnlLiteClient)
            .adminAddress(adminWallet.getAddress())
            .nextAdminAddress(Address.of(NEW_ADMIN2))
            .content(
                NftUtils.createOffChainUriCell(
                    "https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
            .jettonWalletCodeHex(WalletCodes.jettonWalletStableCoin.getValue())
            .build();

    log.info("jetton minter address {}", minter.getAddress());

    // DEPLOY MINTER AND MINT JETTONS

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(1.2))
            .stateInit(minter.getStateInit())
            .body(
                JettonMinterStableCoin.createMintBody(
                    0,
                    adminWallet.getAddress(),
                    Utils.toNano(0.1), // ton amount
                    Utils.toNano(100500), // jetton amount
                    null, // from address
                    null, // response address
                    BigInteger.ONE, // fwd amount
                    MsgUtils.createTextMessageBody("minting")) // forward payload
                )
            .build();

    SendResponse sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(60, "minting...");

    getMinterInfoV2(minter);

    assertThat(minter.getJettonData().getTotalSupply().longValue()).isNotEqualTo(0);

    // TRANSFER from adminWallet to wallet2 (by sending transfer request to admin's jettonWallet)
    JettonWalletStableCoin adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    Utils.toNano(200),
                    wallet2.getAddress(), // recipient
                    null, // response address
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody("gift") // forward payload
                    ))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring 200 jettons from adminWallet to wallet2...");

    getMinterInfoV2(minter);

    // TRANSFER from wallet2 to adminWallet (by sending transfer request to wallet2's jettonWallet)
    JettonWalletStableCoin jettonWallet2 = minter.getJettonWallet(wallet2.getAddress());

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());
    log.info(
        "    jettonWallet2 balance: {}, address: {}",
        Utils.formatNanoValue(jettonWallet2.getBalance(), 6),
        jettonWallet2.getAddress());

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(wallet2.getSeqno())
            .destination(jettonWallet2.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    Utils.toNano(100),
                    adminWallet.getAddress(), // recipient
                    null, // response address
                    Utils.toNano("0.01"), // forward amount
                    MsgUtils.createTextMessageBody("gift from wallet2") // forward payload
                    ))
            .build();

    sendResponse = wallet2.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring 100 jettons from wallet2 to adminWallet...");

    getMinterInfoV2(minter);

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());
    log.info(
        "    jettonWallet2 balance: {}, address: {}",
        Utils.formatNanoValue(jettonWallet2.getBalance(), 6),
        jettonWallet2.getAddress());

    // LOCK JETTONS via minter contract in walletJetton2

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.07))
            .body(
                JettonWalletStableCoin.createCallToBody(
                    0,
                    wallet2.getAddress(),
                    Utils.toNano(0.05),
                    JettonWalletStableCoin.createStatusBody(0, 3) // cant send and receive
                    ))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "locking jettons in jettonWallet2");

    // since the jettonWallet2 is locked the transfer and receive should FAIL
    // TRANSFER from wallet2 to adminWallet (by sending transfer request to wallet2's jettonWallet)
    jettonWallet2 = minter.getJettonWallet(wallet2.getAddress());

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(wallet2.getSeqno())
            .destination(jettonWallet2.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    Utils.toNano(50),
                    adminWallet.getAddress(), // recipient
                    null, // response address
                    Utils.toNano("0.01"), // forward amount
                    MsgUtils.createTextMessageBody("gift from wallet2")) // forward payload
                )
            .build();

    sendResponse = wallet2.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring 50 jettons after lock from wallet2 to adminWallet...");

    getMinterInfoV2(minter);

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());
    log.info(
        "    jettonWallet2 balance: {}, address: {}",
        Utils.formatNanoValue(jettonWallet2.getBalance(), 6),
        jettonWallet2.getAddress());

    // in explorer there will be an error Failed Compute Phase (exit_code 45)
    assertThat(jettonWallet2.getBalance()).isEqualTo(Utils.toNano(100));
  }

  @Test
  public void testJettonMinterStableCoinAdnlTonCenter() throws Exception {
    TonCenter tonCenter =
        TonCenter.builder()
            .apiKey(TESTNET_API_KEY)
            .network(Network.TESTNET)
            .uniqueRequests()
            .build();

    adminWallet = GenerateWallet.randomV3R1(tonCenter, 3);
    Utils.sleep(2);
    wallet2 = GenerateWallet.randomV3R1(tonCenter, 2);

    JettonMinterStableCoin minter =
        JettonMinterStableCoin.builder()
            .tonCenterClient(tonCenter)
            .adminAddress(adminWallet.getAddress())
            .nextAdminAddress(Address.of(NEW_ADMIN2))
            .content(
                NftUtils.createOffChainUriCell(
                    "https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
            .jettonWalletCodeHex(WalletCodes.jettonWalletStableCoin.getValue())
            .build();

    log.info("jetton minter address {}", minter.getAddress());

    // DEPLOY MINTER AND MINT JETTONS
    Utils.sleep(2);
    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(1.2))
            .stateInit(minter.getStateInit())
            .body(
                JettonMinterStableCoin.createMintBody(
                    0,
                    adminWallet.getAddress(),
                    Utils.toNano(0.1), // ton amount
                    Utils.toNano(100500), // jetton amount
                    null, // from address
                    null, // response address
                    BigInteger.ONE, // fwd amount
                    MsgUtils.createTextMessageBody("minting")) // forward payload
                )
            .build();

    SendResponse sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(60, "minting...");

    getMinterInfoV2(minter);
    Utils.sleep(2);
    assertThat(minter.getJettonData().getTotalSupply().longValue()).isNotEqualTo(0);
    Utils.sleep(2);
    // TRANSFER from adminWallet to wallet2 (by sending transfer request to admin's jettonWallet)
    JettonWalletStableCoin adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());
    Utils.sleep(2);
    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    Utils.toNano(200),
                    wallet2.getAddress(), // recipient
                    null, // response address
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody("gift") // forward payload
                    ))
            .build();
    Utils.sleep(2);
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring 200 jettons from adminWallet to wallet2...");

    getMinterInfoV2(minter);
    Utils.sleep(2);
    // TRANSFER from wallet2 to adminWallet (by sending transfer request to wallet2's jettonWallet)
    JettonWalletStableCoin jettonWallet2 = minter.getJettonWallet(wallet2.getAddress());

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());
    log.info(
        "    jettonWallet2 balance: {}, address: {}",
        Utils.formatNanoValue(jettonWallet2.getBalance(), 6),
        jettonWallet2.getAddress());

    Utils.sleep(2);
    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(wallet2.getSeqno())
            .destination(jettonWallet2.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    Utils.toNano(100),
                    adminWallet.getAddress(), // recipient
                    null, // response address
                    Utils.toNano("0.01"), // forward amount
                    MsgUtils.createTextMessageBody("gift from wallet2") // forward payload
                    ))
            .build();

    sendResponse = wallet2.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring 100 jettons from wallet2 to adminWallet...");

    getMinterInfoV2(minter);

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());
    log.info(
        "    jettonWallet2 balance: {}, address: {}",
        Utils.formatNanoValue(jettonWallet2.getBalance(), 6),
        jettonWallet2.getAddress());

    // LOCK JETTONS via minter contract in walletJetton2
    Utils.sleep(2);
    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.07))
            .body(
                JettonWalletStableCoin.createCallToBody(
                    0,
                    wallet2.getAddress(),
                    Utils.toNano(0.05),
                    JettonWalletStableCoin.createStatusBody(0, 3) // cant send and receive
                    ))
            .build();
    Utils.sleep(2);
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "locking jettons in jettonWallet2");

    // since the jettonWallet2 is locked the transfer and receive should FAIL
    // TRANSFER from wallet2 to adminWallet (by sending transfer request to wallet2's jettonWallet)
    Utils.sleep(2);
    jettonWallet2 = minter.getJettonWallet(wallet2.getAddress());

    Utils.sleep(2);
    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(wallet2.getSeqno())
            .destination(jettonWallet2.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    Utils.toNano(50),
                    adminWallet.getAddress(), // recipient
                    null, // response address
                    Utils.toNano("0.01"), // forward amount
                    MsgUtils.createTextMessageBody("gift from wallet2")) // forward payload
                )
            .build();

    sendResponse = wallet2.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring 50 jettons after lock from wallet2 to adminWallet...");

    getMinterInfoV2(minter);

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 6),
        adminJettonWallet.getAddress());
    log.info(
        "    jettonWallet2 balance: {}, address: {}",
        Utils.formatNanoValue(jettonWallet2.getBalance(), 6),
        jettonWallet2.getAddress());

    // in explorer there will be an error Failed Compute Phase (exit_code 45)
    assertThat(jettonWallet2.getBalance()).isEqualTo(Utils.toNano(100));
  }

  private void getMinterInfoV2(JettonMinterStableCoin minter) {
    JettonMinterData data = minter.getJettonData();
    log.info("minter adminAddress {}", data.getAdminAddress());
    log.info("minter totalSupply {}", Utils.formatNanoValue(data.getTotalSupply(), 6));
    log.info("minter jetton uri {}", data.getJettonContentUri());
  }
}
