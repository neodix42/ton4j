package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ton.ton4j.smartcontract.integrationtests.CommonTest.TESTNET_API_KEY;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.toncenter.Network;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.smartcontract.GenerateWallet;
import org.ton.ton4j.smartcontract.highload.HighloadWalletV3;
import org.ton.ton4j.smartcontract.token.ft.JettonMinter;
import org.ton.ton4j.smartcontract.token.ft.JettonWallet;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.*;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R1;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestJetton {
  public static final String NEW_ADMIN2 = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";

  static WalletV3R1 adminWallet;
  static WalletV3R1 wallet2;
  static HighloadWalletV3 highloadWallet2;
  static Tonlib tonlib;

  @Test
  public void testJettonMinter() throws InterruptedException {

    tonlib =
        Tonlib.builder().testnet(true).pathToTonlibSharedLib(Utils.getTonlibGithubUrl()).build();
    adminWallet = GenerateWallet.randomV3R1(tonlib, 2);
    wallet2 = GenerateWallet.randomV3R1(tonlib, 1);

    log.info("admin wallet address {}", adminWallet.getAddress());
    log.info("second wallet address {}", wallet2.getAddress());

    JettonMinter minter =
        JettonMinter.builder()
            .tonlib(tonlib)
            .adminAddress(adminWallet.getAddress())
            .content(
                NftUtils.createOffChainUriCell(
                    "https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
            .jettonWalletCodeHex(WalletCodes.jettonWallet.getValue())
            .build();

    log.info("jetton minter address {}", minter.getAddress());

    // DEPLOY MINTER

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.2))
            .stateInit(minter.getStateInit())
            .comment("deploy minter")
            .build();

    SendResponse sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();
    log.info("deploying minter");
    minter.waitForDeployment(60);

    getMinterInfo(minter); // nothing minted, so zero returned

    // MINT JETTONS

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.07))
            .body(
                JettonMinter.createMintBody(
                    0,
                    adminWallet.getAddress(),
                    Utils.toNano(0.07),
                    Utils.toNano(100500),
                    null,
                    null,
                    BigInteger.ONE,
                    MsgUtils.createTextMessageBody("minting")))
            .build();

    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(45, "minting...");

    getMinterInfo(minter);

    // EDIT MINTER'S JETTON CONTENT

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.055))
            .body(
                minter.createEditContentBody(
                    "http://localhost/nft-marketplace/my_collection_1.json", 0))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "edit minter content, OP 4");

    getMinterInfo(minter);

    // CHANGE MINTER ADMIN
    log.info("newAdmin {}", Address.of(NEW_ADMIN2));

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.056))
            .body(minter.createChangeAdminBody(0, Address.of(NEW_ADMIN2)))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "change minter admin, OP 3");

    getMinterInfo(minter);

    Utils.sleep(30);

    log.info("adminWallet balance: {}", Utils.formatNanoValue(adminWallet.getBalance()));
    log.info("    wallet2 balance: {}", Utils.formatNanoValue(wallet2.getBalance()));

    JettonWallet adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());
    log.info("adminJettonWallet {}", adminJettonWallet.getAddress());

    // transfer from admin to WALLET2_ADDRESS by sending transfer request to admin's jetton wallet

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWallet.createTransferBody(
                    0,
                    Utils.toNano(444),
                    wallet2.getAddress(), // recipient
                    null, // response address TODO does not work if filled
                    null, // custom payload
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody("gift") // forward payload
                    ))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring 444 jettons...");
    log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

    // wallet 2, after received jettons, can use JettonWallet
    JettonWallet jettonWallet2 = minter.getJettonWallet(wallet2.getAddress());
    log.info("wallet2 balance {}", Utils.formatNanoValue(jettonWallet2.getBalance()));

    // BURN JETTONS in ADMIN WALLET

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.05))
            .body(JettonWallet.createBurnBody(0, Utils.toNano(111), adminWallet.getAddress()))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "burning 111 jettons in admin wallet");
    log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

    getMinterInfo(minter);
  }

  @Test
  public void testJettonTransferWithHighloadWalletV3_50()
      throws InterruptedException, NoSuchAlgorithmException {

    tonlib = Tonlib.builder().testnet(true).build();

    adminWallet = GenerateWallet.randomV3R1(tonlib, 2);
    highloadWallet2 = GenerateWallet.randomHighloadV3R1(tonlib, 20);

    // DEPLOY MINTER AND MINT JETTONS

    JettonMinter minter =
        JettonMinter.builder()
            .tonlib(tonlib)
            .adminAddress(adminWallet.getAddress())
            .content(
                NftUtils.createOffChainUriCell(
                    "https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
            .jettonWalletCodeHex(WalletCodes.jettonWallet.getValue())
            .build();

    log.info("jetton minter address {}", minter.getAddress());

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(1.2))
            .stateInit(minter.getStateInit())
            .body(
                JettonMinter.createMintBody(
                    0,
                    adminWallet.getAddress(),
                    Utils.toNano(0.1), // ton amount
                    BigInteger.valueOf(100500_00), // jetton amount
                    null, // from address
                    null, // response address
                    BigInteger.ONE, // fwd amount
                    MsgUtils.createTextMessageBody("minting"))) // forward payload
            .build();
    SendResponse sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();
    log.info("deploying minter and minting...");
    minter.waitForDeployment(60);
    Utils.sleep(15);

    getMinterInfo(minter);

    log.info("adminWallet balance: {}", Utils.formatNanoValue(adminWallet.getBalance()));

    // TRANSFER from adminWallet to highloadWallet2 (by sending transfer request to admin's
    // jettonWallet)
    JettonWallet adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());

    log.info(
        "adminJettonWallet balance: {}, address: {}",
        Utils.formatNanoValue(adminJettonWallet.getBalance(), 2),
        adminJettonWallet.getAddress());

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWallet.createTransferBody(
                    0,
                    BigInteger.valueOf(2000_00), // 2 decimals
                    highloadWallet2.getAddress(), // recipient
                    null, // response address
                    null, // custom payload
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody("gift"))) // forward payload
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    Assertions.assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring 2000 jettons from adminWallet to highloadWallet2...");

    JettonWallet highloadJettonWallet2 = minter.getJettonWallet(highloadWallet2.getAddress());

    // transfer jettons from highloadWallet2 to 600 destinations by sending transfer request to
    // highload's jetton wallet

    HighloadV3Config highloadV3Config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .body(
                highloadWallet2.createBulkTransfer(
                    createDummyDestinations(50, highloadJettonWallet2),
                    BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
            .build();
    sendResponse = highloadWallet2.send(highloadV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "transferring to 50 recipients 2 jettons...");
    log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

    getMinterInfo(minter);
  }

  @Test
  public void testJettonMinterAdnlLiteClient() throws Exception {

    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();

    adminWallet = GenerateWallet.randomV3R1(adnlLiteClient, 2);
    wallet2 = GenerateWallet.randomV3R1(adnlLiteClient, 1);

    log.info("admin wallet address {}", adminWallet.getAddress());
    log.info("second wallet address {}", wallet2.getAddress());

    JettonMinter minter =
        JettonMinter.builder()
            .adnlLiteClient(adnlLiteClient)
            .adminAddress(adminWallet.getAddress())
            .content(
                NftUtils.createOffChainUriCell(
                    "https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
            .jettonWalletCodeHex(WalletCodes.jettonWallet.getValue())
            .build();

    log.info("jetton minter address {}", minter.getAddress());

    // DEPLOY MINTER

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.2))
            .stateInit(minter.getStateInit())
            .comment("deploy minter")
            .build();

    SendResponse sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();
    log.info("deploying minter");
    minter.waitForDeployment(60);

    getMinterInfo(minter); // nothing minted, so zero returned

    // MINT JETTONS

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.07))
            .body(
                JettonMinter.createMintBody(
                    0,
                    adminWallet.getAddress(),
                    Utils.toNano(0.07),
                    Utils.toNano(100500),
                    null,
                    null,
                    BigInteger.ONE,
                    MsgUtils.createTextMessageBody("minting")))
            .build();

    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(45, "minting...");

    getMinterInfo(minter);

    // EDIT MINTER'S JETTON CONTENT

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.055))
            .body(
                minter.createEditContentBody(
                    "http://localhost/nft-marketplace/my_collection_1.json", 0))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "edit minter content, OP 4");

    getMinterInfo(minter);

    // CHANGE MINTER ADMIN
    log.info("newAdmin {}", Address.of(NEW_ADMIN2));

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.056))
            .body(minter.createChangeAdminBody(0, Address.of(NEW_ADMIN2)))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "change minter admin, OP 3");

    getMinterInfo(minter);

    Utils.sleep(45);

    log.info("adminWallet balance: {}", Utils.formatNanoValue(adminWallet.getBalance()));
    log.info("    wallet2 balance: {}", Utils.formatNanoValue(wallet2.getBalance()));

    JettonWallet adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());
    log.info("adminJettonWallet {}", adminJettonWallet.getAddress());

    // transfer from admin to WALLET2_ADDRESS by sending transfer request to admin's jetton wallet

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWallet.createTransferBody(
                    0,
                    Utils.toNano(444),
                    wallet2.getAddress(), // recipient
                    null, // response address TODO does not work if filled
                    null, // custom payload
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody("gift") // forward payload
                    ))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(60, "transferring 444 jettons...");
    log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

    // wallet 2, after received jettons, can use JettonWallet
    JettonWallet jettonWallet2 = minter.getJettonWallet(wallet2.getAddress());
    log.info("wallet2 balance {}", Utils.formatNanoValue(jettonWallet2.getBalance()));

    // BURN JETTONS in ADMIN WALLET

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.05))
            .body(JettonWallet.createBurnBody(0, Utils.toNano(111), adminWallet.getAddress()))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "burning 111 jettons in admin wallet");
    log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

    getMinterInfo(minter);
  }
  
  @Test
  public void testJettonMinterTonCenterClient() throws Exception {

    TonCenter tonCenter =
        TonCenter.builder()
            .apiKey(TESTNET_API_KEY)
            .network(Network.TESTNET)
            .build();

    adminWallet = GenerateWallet.randomV3R1(tonCenter, 2);
    wallet2 = GenerateWallet.randomV3R1(tonCenter, 1);

    log.info("admin wallet address {}", adminWallet.getAddress());
    log.info("second wallet address {}", wallet2.getAddress());

    JettonMinter minter =
        JettonMinter.builder()
            .tonCenterClient(tonCenter)
            .adminAddress(adminWallet.getAddress())
            .content(
                NftUtils.createOffChainUriCell(
                    "https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
            .jettonWalletCodeHex(WalletCodes.jettonWallet.getValue())
            .build();

    log.info("jetton minter address {}", minter.getAddress());

    // DEPLOY MINTER

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.2))
            .stateInit(minter.getStateInit())
            .comment("deploy minter")
            .build();

    Utils.sleep(2);

    SendResponse sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();
    log.info("deploying minter");
    minter.waitForDeployment(60);

    getMinterInfo(minter); // nothing minted, so zero returned

    // MINT JETTONS
    Utils.sleep(2);
    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.07))
            .body(
                JettonMinter.createMintBody(
                    0,
                    adminWallet.getAddress(),
                    Utils.toNano(0.07),
                    Utils.toNano(100500),
                    null,
                    null,
                    BigInteger.ONE,
                    MsgUtils.createTextMessageBody("minting")))
            .build();

    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(45, "minting...");

    getMinterInfo(minter);

    // EDIT MINTER'S JETTON CONTENT
    Utils.sleep(2);
    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.055))
            .body(
                minter.createEditContentBody(
                    "http://localhost/nft-marketplace/my_collection_1.json", 0))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "edit minter content, OP 4");

    getMinterInfo(minter);

    // CHANGE MINTER ADMIN
    log.info("newAdmin {}", Address.of(NEW_ADMIN2));

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.056))
            .body(minter.createChangeAdminBody(0, Address.of(NEW_ADMIN2)))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "change minter admin, OP 3");

    getMinterInfo(minter);

    Utils.sleep(45);

    log.info("adminWallet balance: {}", Utils.formatNanoValue(adminWallet.getBalance()));
    log.info("    wallet2 balance: {}", Utils.formatNanoValue(wallet2.getBalance()));

    JettonWallet adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());
    log.info("adminJettonWallet {}", adminJettonWallet.getAddress());

    // transfer from admin to WALLET2_ADDRESS by sending transfer request to admin's jetton wallet

    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWallet.createTransferBody(
                    0,
                    Utils.toNano(444),
                    wallet2.getAddress(), // recipient
                    null, // response address TODO does not work if filled
                    null, // custom payload
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody("gift") // forward payload
                    ))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(60, "transferring 444 jettons...");
    log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

    // wallet 2, after received jettons, can use JettonWallet
    JettonWallet jettonWallet2 = minter.getJettonWallet(wallet2.getAddress());
    log.info("wallet2 balance {}", Utils.formatNanoValue(jettonWallet2.getBalance()));

    // BURN JETTONS in ADMIN WALLET
    Utils.sleep(2);
    walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.05))
            .body(JettonWallet.createBurnBody(0, Utils.toNano(111), adminWallet.getAddress()))
            .build();
    sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(30, "burning 111 jettons in admin wallet");
    log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

    getMinterInfo(minter);
  }

  private void getMinterInfo(JettonMinter minter) {
    JettonMinterData data = minter.getJettonData();
    log.info("minter adminAddress {}", data.getAdminAddress());
    log.info("minter totalSupply {}", data.getTotalSupply());
    log.info("minter jetton uri {}", data.getJettonContentUri());
  }

  List<Destination> createDummyDestinations(int count, JettonWallet jettonWallet)
      throws NoSuchAlgorithmException {
    List<Destination> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String dstDummyAddress = Utils.generateRandomAddress(0);

      result.add(
          Destination.builder()
              .bounce(false)
              .address(jettonWallet.getAddress().toBounceable())
              .amount(Utils.toNano(0.06))
              .body(
                  JettonWallet.createTransferBody(
                      0,
                      BigInteger.valueOf(2_00), // 2 jettons, with decimals = 2
                      Address.of(dstDummyAddress), // recipient
                      null, // response address
                      null, // custom payload
                      BigInteger.ONE, // forward amount
                      MsgUtils.createTextMessageBody("test sdk"))) // forward payload / memo
              .build());
    }
    return result;
  }
}
