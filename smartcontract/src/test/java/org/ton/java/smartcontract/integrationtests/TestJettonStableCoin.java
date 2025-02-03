package org.ton.java.smartcontract.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.GenerateWallet;
import org.ton.java.smartcontract.token.ft.JettonMinterStableCoin;
import org.ton.java.smartcontract.token.ft.JettonWalletStableCoin;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.JettonMinterData;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v3.WalletV3R1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestJettonStableCoin {
  public static final String NEW_ADMIN2 = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";

  static WalletV3R1 adminWallet;
  static WalletV3R1 wallet2;
  static Tonlib tonlib;

  @BeforeClass
  public static void setUpBeforeClass() throws InterruptedException {
    tonlib =
        Tonlib.builder()
            .testnet(true)
            .pathToTonlibSharedLib(
                Utils.getArtifactGithubUrl("tonlibjson", "latest", "neodix42", "ton"))
            .ignoreCache(false)
            .build();
    adminWallet = GenerateWallet.randomV3R1(tonlib, 3);
    wallet2 = GenerateWallet.randomV3R1(tonlib, 2);
  }

  @Test
  public void testJettonMinterStableCoin() {

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

    ExtMessageInfo extMessageInfo = adminWallet.send(walletV3Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

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
    extMessageInfo = adminWallet.send(walletV3Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

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

    extMessageInfo = wallet2.send(walletV3Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

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
    extMessageInfo = adminWallet.send(walletV3Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

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

    extMessageInfo = wallet2.send(walletV3Config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

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
